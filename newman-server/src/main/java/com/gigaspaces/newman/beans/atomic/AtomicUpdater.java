package com.gigaspaces.newman.beans.atomic;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.Query;
import javax.transaction.Transactional;
import java.util.*;

@Component
@Scope("prototype")
@Transactional
public class AtomicUpdater<T> {

    private final Class<T> entityClass;

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
        return where("e.id = ?", id);
    }

    // ---------- EXECUTE ----------
    public T execute() {
        if (sets.isEmpty() && incs.isEmpty() && decs.isEmpty()) {
            throw new IllegalStateException("Nothing to update");
        }

        StringBuilder jpql = new StringBuilder("UPDATE " + entityClass.getSimpleName() + " e SET ");
        List<String> updates = new ArrayList<>();

        sets.forEach((f, v) -> {
            String p = "p" + (paramCounter++);
            updates.add("e." + f + " = :" + p);
            params.put(p, v);
        });

        incs.forEach((f, v) -> {
            String p = "p" + (paramCounter++);
            updates.add("e." + f + " = e." + f + " + :" + p);
            params.put(p, v);
        });

        decs.forEach((f, v) -> {
            String p = "p" + (paramCounter++);
            updates.add("e." + f + " = e." + f + " - :" + p);
            params.put(p, v);
        });

        jpql.append(String.join(", ", updates));

        if (whereClause != null && !whereClause.isEmpty()) {
            jpql.append(" WHERE ").append(whereClause);
        }

        if (entityManager != null) {
            Query q = entityManager.createQuery(jpql.toString());
            params.forEach(q::setParameter);

            EntityTransaction tx = entityManager.getTransaction();
            try {
                tx.begin();
                int updateRows = q.executeUpdate();
                tx.commit();

                T updatedEntity = null;
                if (updateRows == 1) {
                    entityManager.clear();
                    updatedEntity = entityManager.find(entityClass, id);
                }

                return updatedEntity;
            } catch (Exception e) {
                if (entityManager.getTransaction().isActive())
                    entityManager.getTransaction().rollback();

                throw e;
            } finally {
                entityManager.close();
            }
        } else {
            String result = jpql.toString();
            for (Map.Entry<String, Object> e : params.entrySet()) {
                result = result.replace(":"+e.getKey(), String.valueOf(e.getValue()));
            }
            System.out.println("Execute:   " + result);
        }
        return null;
    }
}

