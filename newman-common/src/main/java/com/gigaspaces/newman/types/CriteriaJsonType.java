package com.gigaspaces.newman.types;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gigaspaces.newman.beans.criteria.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.usertype.UserType;
import org.postgresql.util.PGobject;

import java.io.IOException;
import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Objects;

public class CriteriaJsonType implements UserType {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final Class<?> clazz = Criteria.class;

    @Override
    public int[] sqlTypes() {
        return new int[] { Types.OTHER };  // Use Types.OTHER for handling the PostgreSQL `json` type
    }

    @Override
    public Class returnedClass() {
        return Criteria.class;  // We treat it as a String in Java for simplicity
    }

    @Override
    public boolean equals(Object x, Object y) throws HibernateException {
        return Objects.equals(x, y);
    }

    @Override
    public int hashCode(Object x) throws HibernateException {
        return x.hashCode();
    }

    @Override
    public Object nullSafeGet(ResultSet rs, String[] names, SharedSessionContractImplementor session, Object owner)
            throws HibernateException, SQLException {
        String json = rs.getString(names[0]);
        if (json == null) {
            return null;
        }
        try {
            return objectMapper.readValue(json, clazz);  // Deserialize the JSON string into the specified class
        } catch (IOException e) {
            throw new HibernateException("Error deserializing JSON to " + clazz.getName(), e);
        }
    }

    @Override
    public void nullSafeSet(PreparedStatement st, Object value, int index, SharedSessionContractImplementor session)
            throws HibernateException, SQLException {
        if (value != null) {
            try {
                String json = objectMapper.writeValueAsString(value);  // Serialize the object into a JSON string
                PGobject pgObject = new PGobject();
                pgObject.setType("json");
                pgObject.setValue(json);

                st.setObject(index, pgObject, Types.OTHER);  // Set the JSON string to the prepared statement
            } catch (IOException e) {
                throw new HibernateException("Error serializing object to JSON", e);
            }
        } else {
            st.setNull(index, Types.OTHER);  // Set null if the value is null
        }
    }

    @Override
    public Object deepCopy(Object value) throws HibernateException {
        try {
            return objectMapper.readValue(objectMapper.writeValueAsString(value), clazz);  // Deep copy using serialization
        } catch (IOException e) {
            throw new HibernateException("Error copying object", e);
        }
    }

    @Override
    public boolean isMutable() {
        return false;
    }

    @Override
    public Serializable disassemble(Object value) throws HibernateException {
        return (Serializable) value;
    }

    @Override
    public Object assemble(Serializable cached, Object owner) throws HibernateException {
        return cached;
    }

    @Override
    public Object replace(Object original, Object target, Object owner) throws HibernateException {
        return original;
    }
}
