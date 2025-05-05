package com.gigaspaces.newman.types;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.usertype.UserType;

import java.io.Serializable;
import java.sql.*;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class SetStringArrayType implements UserType {

    @Override
    public int[] sqlTypes() {
        return new int[]{Types.ARRAY};
    }

    @Override
    @SuppressWarnings("unchecked")
    public Class<Set<String>> returnedClass() {
        return (Class<Set<String>>) (Class<?>) Set.class;
    }

    @Override
    public boolean equals(Object x, Object y) throws HibernateException {
        if (x instanceof Set && y instanceof Set) {
            return x.equals(y);
        }
        return false;
    }

    @Override
    public int hashCode(Object x) throws HibernateException {
        return x.hashCode();
    }

    @Override
    public Object nullSafeGet(ResultSet rs, String[] names, SharedSessionContractImplementor session, Object owner)
            throws HibernateException, SQLException {
        Array array = rs.getArray(names[0]);
        if (array != null) {
            String[] values = (String[]) array.getArray();
            Set<String> resultSet = new HashSet<>(Arrays.asList(values));
            return resultSet;
        }
        return null;
    }

    @Override
    public void nullSafeSet(PreparedStatement st, Object value, int index, SharedSessionContractImplementor session)
            throws HibernateException, SQLException {
        if (value != null) {
            Set<String> set = (Set<String>) value;
            String[] array = set.toArray(new String[0]);
            Array sqlArray = session.connection().createArrayOf("text", array);
            st.setArray(index, sqlArray);
        } else {
            st.setNull(index, sqlTypes()[0]);
        }
    }

    @Override
    public Object deepCopy(Object value) throws HibernateException {
        Set<String> set = (Set<String>) value;
        return new HashSet<>(set);
    }

    @Override
    public boolean isMutable() {
        return true;
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
