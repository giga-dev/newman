package com.gigaspaces.newman.types;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.usertype.UserType;

import java.io.Serializable;
import java.sql.*;
import java.util.*;

public class ListStringArrayType implements UserType {

    @Override
    public int[] sqlTypes() {
        return new int[]{Types.ARRAY};
    }

    @Override
    @SuppressWarnings("unchecked")
    public Class<List<String>> returnedClass() {
        return (Class<List<String>>) (Class<?>) Set.class;
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
            List<String> resultSet = new ArrayList<>(Arrays.asList(values));
            return resultSet;
        }
        return null;
    }

    @Override
    public void nullSafeSet(PreparedStatement st, Object value, int index, SharedSessionContractImplementor session)
            throws HibernateException, SQLException {
        if (value != null) {
            List<String> list = (List<String>) value;
            String[] array = list.toArray(new String[0]);
            Array sqlArray = session.connection().createArrayOf("text", array);
            st.setArray(index, sqlArray);
        } else {
            st.setNull(index, sqlTypes()[0]);
        }
    }

    @Override
    public Object deepCopy(Object value) throws HibernateException {
        List<String> set = (List<String>) value;
        return new ArrayList<>(set);
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
