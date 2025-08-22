package com.gigaspaces.newman.beans.atomic;

import com.gigaspaces.newman.entities.BuildsCache;
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
    private final Map<String, Object> removes = new HashMap<>();
    private final Map<String, Object> adds = new HashMap<>();

    private String whereClause;
    private final Map<String, Object> params = new LinkedHashMap<>();
    private final EntityManager entityManager;
    private int paramCounter = 0;
    private String id;
    private final Map<String,Object[]> updatesKeyValue = new HashMap<>();

    public AtomicUpdater(Class<T> entityClass, EntityManager entityManager) {
        this.entityClass = entityClass;
        this.entityManager = entityManager;
    }

    public AtomicUpdater<T> add(String field, Object value) {
        adds.put(field, value);
        return this;
    }

    public AtomicUpdater<T> remove(String field, Object value) {
        removes.put(field, value);
        return this;
    }

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

    // key = JSON object key, value = JSON object value
    public AtomicUpdater<T> putKeyValue(String fieldName, String key, Object value) {
        String pKeyIndex = "p" + (paramCounter++);
        String pValIndex = "p" + (paramCounter++);
        String sqlExpr = String.format(
                "%s = COALESCE(%s, '{}') || jsonb_build_object(:%s, :%s)",
                toColumnName(fieldName),         // column name
                toColumnName(fieldName),         // column name
                pKeyIndex, pValIndex
        );
        updatesKeyValue.put(sqlExpr, new Object[]{pKeyIndex, pValIndex, key, value});
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
        String result = sql.toString();
        for (Map.Entry<String, Object> e : params.entrySet()) {
            result = result.replace(":" + e.getKey(), String.valueOf(e.getValue()));
        }
        if (entityManager != null) {
            logger.info("Executing update: {}", result);
        } else {
            System.out.println("Executing update: " + result);
        }
    }

    private void convertType(String name, Object value, Query query) {
        if (value instanceof java.util.Date && !(value instanceof java.sql.Timestamp)) {
            query.setParameter(name, new java.sql.Timestamp(((java.util.Date) value).getTime()));
        } else if (value instanceof Enum) {
            query.setParameter(name, ((Enum<?>) value).name());
        } else {
            query.setParameter(name, value);
        }
    }

    // ---------- EXECUTE ----------
    public int execute() {
        if (sets.isEmpty() && incs.isEmpty() && decs.isEmpty() && removes.isEmpty() && adds.isEmpty() && updatesKeyValue.isEmpty()) {
            throw new IllegalStateException("Nothing to update");
        }

        // Build native SQL
        StringBuilder sql = new StringBuilder("UPDATE " + getTableName(entityClass) + " SET ");
        List<String> updates = new ArrayList<>();

        // Simple assignments
        sets.forEach((f, v) -> {
            if (v == null) {
                updates.add(toColumnName(f) + " = NULL");
            } else {
                String p = "p" + (paramCounter++);
                updates.add(toColumnName(f) + " = :" + p);
                params.put(p, v);
            }
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

        removes.forEach((f, v) -> {
            String p = "p" + (paramCounter++);
            updates.add(toColumnName(f) + " = array_remove(" + toColumnName(f) + ", :" + p + ")");
            params.put(p, v);
        });

        adds.forEach((f, v) -> {
            String p = "p" + (paramCounter++);
            updates.add(toColumnName(f) + " = array_append(" + toColumnName(f) + ", :" + p + ")");
            params.put(p, v);
        });

        updatesKeyValue.forEach((expr, kv) -> {
            updates.add(expr);
            String pKeyIndex = (String) kv[0];
            String pValIndex = (String) kv[1];
            Object key = kv[2];
            Object val = kv[3];

            params.put(pKeyIndex, key);
            params.put(pValIndex, val);
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

