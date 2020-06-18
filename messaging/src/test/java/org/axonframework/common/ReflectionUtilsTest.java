/*
 * Copyright (c) 2010-2018. Axon Framework
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.axonframework.common;

import org.junit.jupiter.api.*;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.axonframework.common.ReflectionUtils.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Allard Buijze
 * @since 0.7
 */
public class ReflectionUtilsTest {

    @Test
    void testFindFieldsInClass() {
        Iterable<Field> actualFields = ReflectionUtils.fieldsOf(SomeSubType.class);
        int t = 0;
        for (Field actual : actualFields) {
            if (actual.isSynthetic()) {
                // this test is probably running with ByteCode manipulation. We ignore synthetic fields.
                continue;
            }
            switch (t++) {
                case 0:
                    assertEquals("field3", actual.getName());
                    break;
                case 1:
                case 2:
                    assertTrue("field1".equals(actual.getName()) || "field2".equals(actual.getName()),
                            "Expected either field1 or field2, but got " + actual.getName()
                                                        + " declared in " + actual.getDeclaringClass().getName());
                    break;
            }
        }
        assertTrue(t >= 2);
    }

    @Test
    void testFindMethodsInClass() {
        Iterable<Method> actualMethods = ReflectionUtils.methodsOf(SomeSubType.class);
        int t = 0;
        for (Method actual : actualMethods) {
            if (actual.isSynthetic()) {
                //  this test is probably running with bytecode manipulation. We ignore synthetic methods.
                continue;
            }
            switch (t++) {
                case 0:
                    assertEquals("getField3", actual.getName());
                    break;
                case 1:
                    assertEquals("getField3", actual.getName());
                    assertEquals("SomeSubInterface", actual.getDeclaringClass().getSimpleName());
                    break;
                case 2:
                case 3:
                    assertTrue("getField1".equals(actual.getName()) || "getField2".equals(actual.getName()),
                            "Expected either getField1 or getField2, but got " + actual.getName()
                                                           + " declared in " + actual.getDeclaringClass().getName());
                    break;
                case 4:
                    assertEquals("SomeInterface", actual.getDeclaringClass().getSimpleName());
                    break;
            }
        }
        assertTrue(t >= 4);
    }

    @Test
    void testGetFieldValue() throws NoSuchFieldException {
        Object value = ReflectionUtils.getFieldValue(SomeType.class.getDeclaredField("field1"), new SomeSubType());
        assertEquals("field1", value);
    }

    @Test
    void testSetFieldValue() throws Exception {
        int expectedFieldValue = 4;
        SomeSubType testObject = new SomeSubType();
        ReflectionUtils.setFieldValue(SomeSubType.class.getDeclaredField("field3"), testObject, expectedFieldValue);
        assertEquals(expectedFieldValue, testObject.getField3());
    }

    @Test
    void testIsAccessible() throws NoSuchFieldException {
        Field field1 = SomeType.class.getDeclaredField("field1");
        Field field2 = SomeType.class.getDeclaredField("field2");
        Field field3 = SomeSubType.class.getDeclaredField("field3");
        assertFalse(ReflectionUtils.isAccessible(field1));
        assertFalse(ReflectionUtils.isAccessible(field2));
        assertTrue(ReflectionUtils.isAccessible(field3));
    }

    @Test
    void testExplicitlyUnequal_NullValues() {
        assertFalse(explicitlyUnequal(null, null));
        assertTrue(explicitlyUnequal(null, ""));
        assertTrue(explicitlyUnequal("", null));
    }

    @Test
    void testHasEqualsMethod() {
        assertTrue(hasEqualsMethod(String.class));
        assertTrue(hasEqualsMethod(ArrayList.class));
        assertFalse(hasEqualsMethod(SomeType.class));
    }

    @SuppressWarnings("RedundantStringConstructorCall")
    @Test
    void testExplicitlyUnequal_ComparableValues() {
        assertFalse(explicitlyUnequal("value", new String("value")));
        assertTrue(explicitlyUnequal("value1", "value2"));
    }

    @Test
    void testExplicitlyUnequal_OverridesEqualsMethod() {
        assertFalse(explicitlyUnequal(Collections.singletonList("value"), Collections.singletonList("value")));
        assertTrue(explicitlyUnequal(Collections.singletonList("value1"), Collections.singletonList("value")));
    }

    @Test
    void testExplicitlyUnequal_NoEqualsOrComparable() {
        assertFalse(explicitlyUnequal(new SomeType(), new SomeType()));
    }

    @Test
    void testResolvePrimitiveWrapperTypeForLong() {
        assertEquals(Long.class, resolvePrimitiveWrapperType(long.class));
    }

    @Test
    void testGetMemberValueFromField() throws NoSuchFieldException, NoSuchMethodException {
        assertEquals("field1",
                     ReflectionUtils.getMemberValue(SomeType.class.getDeclaredField("field1"), new SomeSubType()));
    }

    @Test
    void testGetMemberValueFromMethod() throws NoSuchMethodException {
        assertEquals("field1",
                     ReflectionUtils.getMemberValue(SomeType.class.getDeclaredMethod("getField1"), new SomeSubType()));
        assertEquals("someMethodResult",
                     ReflectionUtils.getMemberValue(SomeTypeWithMethods.class.getDeclaredMethod("someMethod"), new SomeTypeWithMethods()));
    }

    @Test
    void testGetMemberValueFromVoidMethod() throws NoSuchMethodException {
        SomeTypeWithMethods testObject = new SomeTypeWithMethods();
        Object voidReturnValue = getMemberValue(SomeTypeWithMethods.class.getDeclaredMethod("someVoidMethod"),
                                                testObject);
        assertNull(voidReturnValue);
        assertEquals(1, testObject.voidMethodInvocations.get());
    }

    @Test
    void testGetMemberValueFromConstructor() {
        assertThrows(IllegalStateException.class,
                     () -> ReflectionUtils.getMemberValue(SomeType.class.getDeclaredConstructor(), new SomeSubType()));
    }

    @Test
    void testGetMemberValueTypeFromField() throws NoSuchFieldException {
        Class<?> fieldType = getMemberValueType(SomeType.class.getDeclaredField("field1"));
        assertEquals(String.class, fieldType);
    }

    @Test
    void testGetMemberValueTypeFromMethod() throws NoSuchMethodException {
        Class<?> methodValueType = getMemberValueType(SomeSubType.class.getDeclaredMethod("getField3"));
        assertEquals(int.class, methodValueType);
        Class<?> voidResultType = getMemberValueType(SomeTypeWithMethods.class.getDeclaredMethod("someVoidMethod"));
        assertEquals(void.class, voidResultType);
    }

    @Test
    void testGetMemberValueTypeFromConstructor() {
        assertThrows(IllegalStateException.class, () -> getMemberValueType(SomeType.class.getDeclaredConstructor()));
    }

    @Test
    void testInvokeAndGetMethodValue() throws NoSuchMethodException {
        assertEquals("field1",
                     invokeAndGetMethodValue(SomeTypeWithMethods.class.getDeclaredMethod("getField1"), new SomeTypeWithMethods()));
        assertEquals("someMethodResult",
                     invokeAndGetMethodValue(SomeTypeWithMethods.class.getDeclaredMethod("someMethod"), new SomeTypeWithMethods()));

        SomeTypeWithMethods testObject = new SomeTypeWithMethods();
        Object voidMethodResult = invokeAndGetMethodValue(SomeTypeWithMethods.class.getDeclaredMethod("someVoidMethod"),
                                                          testObject);
        assertNull(voidMethodResult);
        assertEquals(1, testObject.voidMethodInvocations.get());
    }

    @Test
    void testMemberGenericTypeFromField() throws NoSuchFieldException {
        Field field = SomeType.class.getDeclaredField("field1");
        Type memberGenericType = getMemberGenericType(field);
        assertEquals(field.getGenericType(), memberGenericType);
    }

    @Test
    void testMemberGenericTypeFromMethod() throws NoSuchMethodException {
        Method method = SomeTypeWithMethods.class.getDeclaredMethod("someMethod");
        Type memberGenericType = getMemberGenericType(method);
        assertEquals(method.getGenericReturnType(), memberGenericType);
    }

    @Test
    void testMemberGenericTypeFromConstructor() throws NoSuchMethodException {
        Constructor<SomeTypeWithMethods> constructor = SomeTypeWithMethods.class.getDeclaredConstructor();
        assertThrows(IllegalStateException.class, () -> getMemberGenericType(constructor));
    }

    @Test
    void testMemberGenericStringFromField() throws NoSuchFieldException {
        Field field = SomeType.class.getDeclaredField("field1");
        String memberGenericString = getMemberGenericString(field);
        assertEquals(field.toGenericString(), memberGenericString);
    }

    @Test
    void testMemberGenericStringFromMethod() throws NoSuchMethodException {
        Method method = SomeTypeWithMethods.class.getDeclaredMethod("someMethod");
        String memberGenericString = getMemberGenericString(method);
        assertEquals(method.toGenericString(), memberGenericString);
    }

    @Test
    void testMemberGenericStringFromConstructor() throws NoSuchMethodException {
        Constructor<SomeTypeWithMethods> constructor = SomeTypeWithMethods.class.getDeclaredConstructor();
        String memberGenericString = getMemberGenericString(constructor);
        assertEquals(constructor.toGenericString(), memberGenericString);
    }

    private static class SomeType implements SomeInterface {

        private String field1 = "field1";
        private String field2 = "field2";

        @Override
        public String getField1() {
            return field1;
        }

        public String getField2() {
            return field2;
        }
    }

    public interface SomeInterface {
        String getField1();
    }

    public interface SomeSubInterface {
        int getField3();

    }

    public static class SomeSubType extends SomeType implements SomeSubInterface {

        public int field3 = 3;

        @Override
        public int getField3() {
            return field3;
        }

    }

    public static class ContainsCollectionsType extends SomeType {

        private List<String> listOfStrings;
        private Map<String, String> mapOfStringToString;
        private Set<String> setOfStrings;

        public ContainsCollectionsType(List<String> listOfStrings, Map<String, String> mapOfStringToString,
                                       Set<String> setOfStrings) {
            this.listOfStrings = listOfStrings;
            this.mapOfStringToString = mapOfStringToString;
            this.setOfStrings = setOfStrings;
        }

        public List<String> getListOfStrings() {
            return listOfStrings;
        }

        public Map<String, String> getMapOfStringToString() {
            return mapOfStringToString;
        }

        public Set<String> getSetOfStrings() {
            return setOfStrings;
        }
    }
    private static class SomeTypeWithMethods implements SomeInterface {

        public AtomicInteger voidMethodInvocations = new AtomicInteger();

        @Override
        public String getField1() {
            return "field1";
        }

        public String someMethod() {
            return "someMethodResult";
        }

        public void someVoidMethod() {
            voidMethodInvocations.incrementAndGet();
        }
    }
}
