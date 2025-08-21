package com.gigaspaces.newman.beans.atomic;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.persistence.*;
import javax.transaction.Transactional;
import java.util.*;

@Component
@Scope("prototype")
@Transactional
public class AtomicUpdater<T> {
    private static final Logger logger = LoggerFactory.getLogger(AtomicUpdater.class);
    private Class<T> entityClass;

    private final Map<String, Object> sets = new LinkedHashMap<>();
    private final Map<String, Number> incs = new LinkedHashMap<>();
    private final Map<String, Number> decs = new LinkedHashMap<>();

    private String whereClause;
    private final Map<String, Object> params = new LinkedHashMap<>();
    private final EntityManager entityManager;
    private int paramCounter = 0;
    private String id;

    public AtomicUpdater(Class<T> entityClass, EntityManager entityManager) {
        this.entityClass = entityClass;
        this.entityManager = entityManager;
    }

    // ---------- SET / INC / DEC ----------
    public AtomicUpdater<T> set(String field, Object value) {
        sets.put(field, value);
        return this;
    }

    public AtomicUpdater<T> inc(String field, Number value) {
        incs.put(field, value);
        return this;
    }

    public AtomicUpdater<T> inc(String field) {
        incs.put(field, 1);
        return this;
    }

    public AtomicUpdater<T> dec(String field, Number value) {
        decs.put(field, value);
        return this;
    }

    public AtomicUpdater<T> dec(String field) {
        decs.put(field, 1);
        return this;
    }

    // ---------- WHERE ----------
    // e.g.:  update.where("field1 = ? AND field2 > ? AND status = ?", 123, 10, "ACTIVE");
    public AtomicUpdater<T> where(String clause, Object... values) {
        whereClause = clause;
        for (Object v : values) {
            String pName = "p" + (paramCounter++);
            whereClause = whereClause.replaceFirst("\\?", ":" + pName);
            params.put(pName, v);
        }
        return this;
    }

    public AtomicUpdater<T> whereId(Object id) {
        this.id = String.valueOf(id);
        return where("id = ?", id);
    }

    private String toColumnName(String fieldName) {
        return fieldName.toLowerCase();
    }

    private String getTableName(Class<?> entityClass) {
        return entityClass.getSimpleName().replaceAll("([a-z])([A-Z])", "$1_$2")  // insert underscore before uppercase
                .toLowerCase();
    }

    private void printSQL(StringBuilder sql) {
        if (logger.isDebugEnabled()) {
            String result = sql.toString();
            for (Map.Entry<String, Object> e : params.entrySet()) {
                result = result.replace(":" + e.getKey(), String.valueOf(e.getValue()));
            }
            if (entityManager != null) {
                logger.debug("Executing update: {}", result);
            } else {
                System.out.println("Executing update: " + result);
            }
        }
    }

    private void convertType(String name, Object value, Query query) {
        if (value == null) {
            if (name.toLowerCase().contains("time")) {
                query.setParameter(name, (java.sql.Timestamp) null); // null timestamp
            } else {
                query.setParameter(name, null);
            }
        } else if (value instanceof java.util.Date && !(value instanceof java.sql.Timestamp)) {
            query.setParameter(name, new java.sql.Timestamp(((java.util.Date) value).getTime()));
        } else if (value instanceof Enum) {
            query.setParameter(name, ((Enum<?>) value).name());
        } else {
            query.setParameter(name, value);
        }
    }

    public int execute(String strQuery) {
        if (entityManager != null) {
            logger.info(strQuery);

            EntityTransaction tx = entityManager.getTransaction();
            try {
                tx.begin();

                Query query = entityManager.createNativeQuery(strQuery);
                int rowsUpdated = query.executeUpdate(); // no entity mapping, just affected rows return
                tx.commit();
                return rowsUpdated; // return number of rows updated instead of entity
            } catch (Exception e) {
                if (tx.isActive()) tx.rollback();
                logger.warn("Failed to execute update", e);
                throw e;
            } finally {
                entityManager.close();
            }
        }
        return -1;
    }

    // ---------- EXECUTE ----------
    public int execute() {
        if (sets.isEmpty() && incs.isEmpty() && decs.isEmpty()) {
            throw new IllegalStateException("Nothing to update");
        }

        // Build native SQL
        StringBuilder sql = new StringBuilder("UPDATE " + getTableName(entityClass) + " SET ");
        List<String> updates = new ArrayList<>();

        // Simple assignments
        sets.forEach((f, v) -> {
            String p = "p" + (paramCounter++);
            updates.add(toColumnName(f) + " = :" + p);
            params.put(p, v);
        });

        // Increments
        incs.forEach((f, v) -> {
            String p = "p" + (paramCounter++);
            updates.add(toColumnName(f) + " = " + toColumnName(f) + " + :" + p);
            params.put(p, v);
        });

        // Decrements
        decs.forEach((f, v) -> {
            String p = "p" + (paramCounter++);
            updates.add(toColumnName(f) + " = " + toColumnName(f) + " - :" + p);
            params.put(p, v);
        });

        sql.append(String.join(", ", updates));

        // WHERE clause
        if (whereClause != null && !whereClause.isEmpty()) {
            sql.append(" WHERE ").append(whereClause);
        }

        if (entityManager != null) {
            printSQL(sql);

            EntityTransaction tx = entityManager.getTransaction();
            try {
                tx.begin();

                Query query = entityManager.createNativeQuery(sql.toString());
                params.forEach((name, value) -> convertType(name, value, query));

                int rowsUpdated = query.executeUpdate(); // no entity mapping, just affected rows return
                tx.commit();
                return rowsUpdated; // return number of rows updated instead of entity
            } catch (Exception e) {
                if (tx.isActive()) tx.rollback();
                logger.warn("Failed to execute update", e);
                throw e;
            } finally {
                entityManager.close();
            }
        } else {
            printSQL(sql);
        }

        return 0;
    }
}

