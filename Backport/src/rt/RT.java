/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package rt;

import specialization.BackClassVisitor;
import specialization.FrontClassVisitor;
import sun.misc.Unsafe;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;

import java.io.IOException;
import java.lang.invoke.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

/**
 * Bootstrap class for virtual access adaptations
 */
public class RT {

    public static final MethodType SPECIALIZED_CONSTRUCTOR_TYPE = MethodType.methodType(void.class, Void.class, Object.class);
    private static final Unsafe UNSAFE = initUnsafe();
    private static final MethodHandle LOOKUP_CONSTRUCTOR_MH = initLookupConstructor();
    private static final String ANY_PACKAGE = BackClassVisitor.ANY_PACKAGE;
    private static final String BACK_FACTORY_NAME = BackClassVisitor.BACK_FACTORY_NAME;

    public static final MethodType TYPE_BSM_GETFIELD = MethodType.methodType(CallSite.class, MethodHandles.Lookup.class, String.class, MethodType.class, String.class, MethodHandles.Lookup.class);
    public static final MethodType TYPE_BSM_PUTFIELD = MethodType.methodType(CallSite.class, MethodHandles.Lookup.class, String.class, MethodType.class, String.class, MethodHandles.Lookup.class);
    public static final MethodType TYPE_BSM_INVOKE_SPECIAL_FROM_BACK = MethodType.methodType(CallSite.class, MethodHandles.Lookup.class, String.class, MethodType.class, String.class, MethodHandles.Lookup.class);
    public static final MethodType TYPE_NO_LOOKUP_BSM_GETFIELD = MethodType.methodType(CallSite.class, MethodHandles.Lookup.class, String.class, MethodType.class, String.class);
    public static final MethodType TYPE_NO_LOOKUP_BSM_PUTFIELD = MethodType.methodType(CallSite.class, MethodHandles.Lookup.class, String.class, MethodType.class, String.class);
    public static final MethodType TYPE_PUTFIELD = MethodType.methodType(MethodHandle.class, MethodHandles.Lookup.class, String.class, Class.class, Object.class);
    public static final MethodType TYPE_GETFIELD = MethodType.methodType(MethodHandle.class, MethodHandles.Lookup.class, String.class, Class.class, Object.class);
    public static final MethodType TYPE_METAFACTORY = MethodType.methodType(CallSite.class, MethodHandles.Lookup.class, String.class, MethodType.class, MethodHandles.Lookup.class, String.class);
    public static final MethodType TYPE_BSM_CREATE_ANY = MethodType.methodType(CallSite.class, MethodHandles.Lookup.class, String.class, MethodType.class, String.class, MethodHandles.Lookup.class);
    public static final MethodType TYPE_NO_LOOKUP_BSM_CREATE_ANY = MethodType.methodType(CallSite.class, MethodHandles.Lookup.class, String.class, MethodType.class, String.class);
    public static final MethodType TYPE_INVOKE_SPECIAL_FROM_BACK = MethodType.methodType(MethodHandle.class, MethodHandles.Lookup.class, String.class, MethodType.class, Object.class);
    public static final MethodType TYPE_INVOKE_INLINED_CALL = MethodType.methodType(MethodHandle.class, MethodHandles.Lookup.class, String.class, MethodType.class, Object.class);

    public static final MethodType BSMS_TYPE = MethodType.methodType(CallSite.class, MethodHandles.Lookup.class, String.class, MethodType.class);
    private static final MethodType INVOKE_FRONT_TYPE = MethodType.methodType(Object.class, MethodHandles.Lookup.class, MethodHandle.class, Class.class, Object[].class);
    private static final MethodType INVOKE_CALL_TYPE = MethodType.methodType(Object.class, MethodHandles.Lookup.class, MethodType.class, String.class, Object.class, Object[].class);
    private static final ClassValue<byte[]> BACK_FACTORY = new ClassValue<byte[]>() {
        @Override
        protected byte[] computeValue(Class<?> type) {
            String backName = ANY_PACKAGE + type.getName() + BACK_FACTORY_NAME + ".class";
            try {
                return Files.readAllBytes(Paths.get(backName));
            } catch (IOException e) {
                throw new IllegalStateException(e.getMessage());
            }
        }
    };

    /**
     * @return an adapted methodHandle to retrieve the lookup of any class.
     */
    private static MethodHandle initLookupConstructor() {
        String PROXY_CLASS = "yv66vgAAADMAGgoABQASBwAUCgACABUHABYHABcBAAY8aW5pdD4BAAMoKVYB"
                + "AARDb2RlAQAPTGluZU51bWJlclRhYmxlAQAGbG9va3VwAQAGTG9va3VwAQAM"
                + "SW5uZXJDbGFzc2VzAQA6KExqYXZhL2xhbmcvQ2xhc3M7KUxqYXZhL2xhbmcv"
                + "aW52b2tlL01ldGhvZEhhbmRsZXMkTG9va3VwOwEACVNpZ25hdHVyZQEAPShM"
                + "amF2YS9sYW5nL0NsYXNzPCo+OylMamF2YS9sYW5nL2ludm9rZS9NZXRob2RI"
                + "YW5kbGVzJExvb2t1cDsBAApTb3VyY2VGaWxlAQAQTG9va3VwUHJveHkuamF2"
                + "YQwABgAHBwAYAQAlamF2YS9sYW5nL2ludm9rZS9NZXRob2RIYW5kbGVzJExv"
                + "b2t1cAwABgAZAQAcamF2YS9sYW5nL2ludm9rZS9Mb29rdXBQcm94eQEAEGph"
                + "dmEvbGFuZy9PYmplY3QBAB5qYXZhL2xhbmcvaW52b2tlL01ldGhvZEhhbmRs"
                + "ZXMBABQoTGphdmEvbGFuZy9DbGFzczspVgAhAAQABQAAAAAAAgABAAYABwAB"
                + "AAgAAAAdAAEAAQAAAAUqtwABsQAAAAEACQAAAAYAAQAAAAUACQAKAA0AAgAI"
                + "AAAAIQADAAEAAAAJuwACWSq3AAOwAAAAAQAJAAAABgABAAAABwAOAAAAAgAP"
                + "AAIAEAAAAAIAEQAMAAAACgABAAIAEwALABk=";
        try {
            byte[] array = MyBase64.getDecoder().decode(PROXY_CLASS);
            Class<?> proxy = UNSAFE.defineAnonymousClass(MethodHandles.class, array, null);
            return MethodHandles.lookup().findStatic(proxy, "lookup", MethodType.methodType(Lookup.class, Class.class));
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
        return null;
    }

    /**
     * This bootstrap method returns an adapted callSite to create an instance of a Back Species class.
     *
     * @param lookup      lookup of the calling class
     * @param name        debug name of the current bootstrap method
     * @param type        type of the callSite
     * @param owner       front class'name of the species requested
     * @param genericName the name of the requested species, with its type variable instances. for example <code>Foo<I, Z></code>.
     * @return the callSite allocating the instance of the specialized Back class.
     * @throws Throwable
     */
    @SuppressWarnings("unused")
    public static CallSite bsm_newBackSpecies(MethodHandles.Lookup lookup, String name, MethodType type, String owner, String genericName) throws Throwable {
        Class<?> frontClass = ClassLoader.getSystemClassLoader().loadClass(owner);
        return new ConstantCallSite(createBackSpecies(lookup, type, frontClass, genericName));
    }

    /**
     * This bootstrap method returns an adapted callSite to perform a delegate call on the Back Species instance of the previously placed
     * front instance.
     *
     * @param lookup     lookup of the calling class
     * @param methodName the name of the method invoked
     * @param type       the signature of the method invoked
     * @return the callSite allocating the instance of the specialized Back class.
     * @throws Throwable
     */
    @SuppressWarnings("unused")
    public static CallSite bsm_inlinedBackCall(MethodHandles.Lookup lookup, String methodName, MethodType type) throws Throwable {
        MethodHandle mh = lookup.findStatic(RT.class, "invokeInlinedCall", TYPE_INVOKE_INLINED_CALL);
        mh = mh.bindTo(lookup).bindTo(type).bindTo(methodName);
        return new ConstantCallSite(invokerOf(type.dropParameterTypes(0, 1), mh).asType(type));
    }

    /**
     * This method returns an adapted methodHandle to perform a call of the method <code>methodName</code> of the
     * Back species instance of the given Front instance <code>front</code>.
     *
     * @param lookup     lookup of the calling class
     * @param methodName the name of the method invoked
     * @param type       the signature of the method invoked
     * @param front      the front instance having a Back species field on which the wanted method will be invoked.
     * @return the methodHandle performing the call to the method of the Back species instance.
     * @throws Throwable
     */
    @SuppressWarnings("unused")
    // TODO try to put private here.
    public static MethodHandle invokeInlinedCall(MethodHandles.Lookup lookup, MethodType type, String methodName, Object front) throws Throwable {
        return lookup.findStatic(getBack__(lookup, front).getClass(), methodName, type).bindTo(front);
    }

    /**
     * This bootstrap method returns an adapted callSite to delegate a call to the Back species instance of the enclosing Front
     * instance.
     *
     * @param lookup lookup of the calling class
     * @param name   debug name of the current bootstrap method
     * @param type   the signature of the method invoked
     * @return the callSite allocating the instance of the specialized Back class.
     * @throws Throwable
     */
    @SuppressWarnings("unused")
    public static CallSite bsm_delegateBackCall(MethodHandles.Lookup lookup, String name, MethodType type) throws Throwable {
        MethodHandle mh = lookup.findStatic(RT.class, "invokeCall", INVOKE_CALL_TYPE);
        // Dropping name, receiver to have the delegate method type (-2 for front receiver, delegate target name).
        mh = mh.bindTo(lookup).bindTo(type.dropParameterTypes(0, 2));
        // Collecting trailing arguments inside an Object[] array. (- 2 for name, receiver).
        mh = mh.asCollector(Object[].class, type.parameterCount() - 2).asType(type);
        return new ConstantCallSite(mh);
    }

    // TODO remove this method to prevent the boxing of the return value and the arguments.
    public static Object invokeCall(MethodHandles.Lookup lookup, MethodType type, String methodName, Object receiver, Object... args) throws Throwable {
        // System.out.println("invokeCall : lookup = [" + lookup + "], type = [" + type + "], name = [" + name + "], receiver = [" + receiver + "], args = [" + args + "]");
        MethodHandle mh = lookup.findStatic(receiver.getClass(), methodName, type).asType(type).asSpreader(Object[].class, args.length);
        return mh.invoke(args);
    }

    /**
     * This bootstrap method returns an adapted callSite to create an instance of an Any class. It first creates the Back species class if needed.
     * Then instantiate it and passes the reference as an argument during an instantiation of the Front - any - class.
     * This method has to be called from outside a Back species instance. If not, the Back species instance's lookup will be used
     * to instantiate the Front class, which is not possible since its host class is {@link Object}.
     *
     * @param lookup      lookup of the calling class
     * @param name        debug name of the current bootstrap method
     * @param erasedType  the erased type of the callSite
     * @param genericName the name of the requested species, with its type variable instances. for example <code>Foo<I, Z></code>.
     * @return the callSite allocating the instance of the specialized Back class.
     * @throws Throwable
     */
    @SuppressWarnings("unused")
    public static CallSite bsm_createAnyNoLookup(MethodHandles.Lookup lookup, String name, MethodType erasedType,
                                                 String genericName) throws Throwable {
        return bsm_createAny(lookup, name, erasedType, genericName, null);
    }

    /**
     * This bootstrap method returns an adapted callSite to create an instance of an Any class. It first creates the Back species class if needed.
     * Then instantiate it and passes the reference as an argument during an instantiation of the Front - any - class.
     * If the front lookup is equal to null, the bootstrap default's lookup will be used.
     *
     * @param lookup      lookup of the calling class
     * @param name        debug name of the current bootstrap method
     * @param erasedType  the erased type of the callSite
     * @param genericName the name of the requested species, with its type variable instances. for example <code>Foo<I, Z></code>.
     * @param front       lookup of the front class used during an invocation from a Back species class instance.
     * @return the callSite allocating the instance of the specialized Back class.
     * @throws Throwable
     */
    // TODO remove "name" parameter.
    public static CallSite bsm_createAny(MethodHandles.Lookup lookup, String name, MethodType erasedType,
                                         String genericName, MethodHandles.Lookup front) throws Throwable {
        // System.out.println("bsm_createAny : lookup = [" + lookup + "], name = [" + name + "], erasedType = [" + erasedType + "], genericName = [" + genericName + "], front = [" + front + "]");
        MethodHandles.Lookup l = front == null ? lookup : front;
        String rawAnyName = Type.rawName(genericName);
        Class<?> frontClass = l.lookupClass().getClassLoader().loadClass(rawAnyName);
        // Allocate specialized class - with object for the moment - and return one of its constructor.
        MethodHandle backMH = createBackSpecies(front == null ? computeLookup(frontClass) : front, erasedType, frontClass, genericName);
        MethodHandle frontMH = l.findStatic(RT.class, "createFrontInstance", INVOKE_FRONT_TYPE);
        frontMH = frontMH.bindTo(l).bindTo(backMH).bindTo(frontClass).asCollector(Object[].class, erasedType.parameterCount()).asType(erasedType);
        return new ConstantCallSite(frontMH);
    }

    /**
     * This bootstrap method returns an adapted callSite to get a field of a Front - any - class instance.
     * Erased and not erased description are needed since an invocation of this very method from a Back species class can not
     * know the real field type (due to {@link Object} being the back's host class.
     *
     * @param lookup        lookup of the calling class
     * @param name          debug name of the current bootstrap method
     * @param erasedType    the erased type of the callSite
     * @param notErasedDesc the field's descriptor
     * @return the callSite getting the requested field.
     * @throws Throwable
     */
    @SuppressWarnings("unused")
    public static CallSite bsm_getFieldNoLookup(MethodHandles.Lookup lookup, String name, MethodType erasedType, String notErasedDesc)
            throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException {
        return bsm_getField(lookup, name, erasedType, notErasedDesc, null);
    }

    /**
     * This bootstrap method returns an adapted callSite to get a field of a Front - any - class instance.
     * Erased and not erased description are needed since an invocation of this very method from a Back species class can not
     * know the real field type (due to {@link Object} being the back's host class.
     * If the front lookup is equal to null, the bootstrap default's lookup will be used.
     *
     * @param lookup        lookup of the calling class
     * @param name          the field's name
     * @param erasedType    the erased type of the callSite
     * @param notErasedDesc the field's descriptor
     * @param front         lookup of the front class used during an invocation from a Back species class instance
     * @return the callSite getting the requested field.
     * @throws Throwable
     */
    @SuppressWarnings("unused")
    public static CallSite bsm_getField(MethodHandles.Lookup lookup, String name, MethodType erasedType, String notErasedDesc, MethodHandles.Lookup front)
            throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException {
        MethodHandles.Lookup l = front == null ? lookup : front;
        Class<?> fieldClass = erasedType.returnType();
        MethodHandle mh = l.findStatic(RT.class, "getField", TYPE_GETFIELD);
        mh = mh.bindTo(lookup).bindTo(name).bindTo(fieldClass);
        return new ConstantCallSite(invokerOf(erasedType.dropParameterTypes(0, 1), mh));
    }

    /**
     * This method returns an adapted methodHandle to get a field of the Front - any - class instance <code>front</code>.
     * Erased and not erased description are needed since an invocation of this very method from a Back species class can not
     * know the real field type (due to {@link Object} being the back's host class.
     * If the front lookup is equal to null, the bootstrap default's lookup will be used.
     *
     * @param lookup     lookup of the calling class
     * @param name       the field's name
     * @param fieldClass the field's class
     * @param front      lookup of the front class used during an invocation from a Back species class instance
     * @return the callSite getting the requested field.
     * @throws Throwable
     */
    @SuppressWarnings("unused")
    public static MethodHandle getField(MethodHandles.Lookup lookup, String name, Class<?> fieldClass, Object front) throws Throwable {
        Object back__ = getBack__(lookup, front);
        Lookup backLookup = computeLookup(back__.getClass());
        return backLookup.findGetter(back__.getClass(), name, fieldClass).bindTo(back__);
    }

    /**
     * This bootstrap method returns an adapted callSite to put a field of a Front - any - class instance.
     * Erased and not erased description are needed since an invocation of this very method from a Back species class can not
     * know the real field type (due to {@link Object} being the back's host class.
     *
     * @param lookup        lookup of the calling class
     * @param name          debug name of the current bootstrap method
     * @param erasedType    the erased type of the callSite
     * @param notErasedDesc the field's descriptor
     * @return the callSite putting the requested field.
     * @throws Throwable
     */
    @SuppressWarnings("unused")
    public static CallSite bsm_putFieldNoLookup(MethodHandles.Lookup lookup, String name, MethodType erasedType, String notErasedDesc)
            throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException {
        return bsm_putField(lookup, name, erasedType, notErasedDesc, null);
    }

    /**
     * This bootstrap method returns an adapted callSite to put a field of a Front - any - class instance.
     * Erased and not erased description are needed since an invocation of this very method from a Back species class can not
     * know the real field type (due to {@link Object} being the back's host class.
     * If the front lookup is equal to null, the bootstrap default's lookup will be used.
     *
     * @param lookup        lookup of the calling class
     * @param name          the field's name
     * @param erasedType    the erased type of the callSite
     * @param notErasedDesc the field's descriptor
     * @param front         lookup of the front class used during an invocation from a Back species class instance
     * @return the callSite putting the requested field.
     * @throws Throwable
     */
    @SuppressWarnings("unused")
    public static CallSite bsm_putField(MethodHandles.Lookup lookup, String name, MethodType erasedType, String notErasedDesc, MethodHandles.Lookup front)
            throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException {
        MethodHandles.Lookup l = front == null ? lookup : front;
        Class<?> fieldClass = erasedType.parameterType(1);
        MethodHandle mh = l.findStatic(RT.class, "putField", TYPE_PUTFIELD);
        mh = mh.bindTo(l).bindTo(name).bindTo(fieldClass);
        return new ConstantCallSite(invokerOf(erasedType.dropParameterTypes(0, 1), mh));
    }

    /**
     * This method returns an adapted methodHandle to put a field of the Front - any - class instance <code>front</code>.
     * Erased and not erased description are needed since an invocation of this very method from a Back species class can not
     * know the real field type (due to {@link Object} being the back's host class.
     * If the front lookup is equal to null, the bootstrap default's lookup will be used.
     *
     * @param lookup     lookup of the calling class
     * @param name       the field's name
     * @param fieldClass the field's class
     * @param front      lookup of the front class used during an invocation from a Back species class instance
     * @return the callSite putting the requested field.
     * @throws Throwable
     */
    @SuppressWarnings("unused")
    public static MethodHandle putField(MethodHandles.Lookup lookup, String name, Class<?> fieldClass, Object front) throws Throwable {
        Object back__ = getBack__(lookup, front);
        Lookup backLookup = computeLookup(back__.getClass());
        return backLookup.findSetter(back__.getClass(), name, fieldClass).bindTo(back__);
    }

    /**
     * This bootstrap method returns an adapted callSite to perform an invoke special inside a Back species instance.
     * Since it only has erased types, this bsm is needed to target the correctly typed method from the back instance.
     *
     * @param lookup        lookup of the calling class
     * @param methodName    the method's methodName
     * @param erasedType    the erased type of the callSite
     * @param notErasedDesc the method's descriptor
     * @param front         the front lookup needed to find methods
     * @return the callSite invoking the method wanted.
     * @throws ClassNotFoundException
     * @throws NoSuchMethodException
     * @throws IllegalAccessException if the front lookup is null.
     */
    @SuppressWarnings("unused")
    public static CallSite bsm_invokeSpecialFromBack(MethodHandles.Lookup lookup, String methodName, MethodType erasedType,
                                                     String notErasedDesc, MethodHandles.Lookup front)
            throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException {
        if (front == null) {
            throw new IllegalStateException("Front lookup can not be null during special invocations from a back class.");
        }

        MethodHandle mh;
        // Determining if the receiver is a typeVar or a parameterized type.
        Type methodType = Type.getMethodType(notErasedDesc);
        Type receiver = methodType.getArgumentTypes()[0];
        if (Type.isParameterizedType(receiver)) {
            mh = front.findStatic(RT.class, "invokeInlinedCall", TYPE_INVOKE_INLINED_CALL);
            // Removing the receiver parameterized type information.
            notErasedDesc = Type.translateMethodDescriptor(notErasedDesc);
        } else {
            mh = front.findStatic(RT.class, "invokeSpecialFromBack", TYPE_INVOKE_SPECIAL_FROM_BACK);
        }

        MethodType mt = MethodType.fromMethodDescriptorString(notErasedDesc, front.lookupClass().getClassLoader());
        mh = MethodHandles.insertArguments(mh, 0, front, methodName, mt);
        return new ConstantCallSite(invokerOf(erasedType.dropParameterTypes(0, 1), mh));
    }

    /**
     * This method returns an adapted methodHandle to perform an invoke special inside a Back species instance.
     * Since it only has erased types, this bsm is needed to target the correctly typed method from the back instance.
     *
     * @param lookup     lookup of the calling class
     * @param methodName the method's name
     * @param methodType the method's type
     * @param receiver   the method's receiver
     * @return the methodHandle invoking the method wanted.
     * @throws NoSuchMethodException
     * @throws IllegalAccessException if the front lookup is null.
     */
    @SuppressWarnings("unused")
    public static MethodHandle invokeSpecialFromBack(Lookup lookup, String methodName, MethodType methodType, Object receiver)
            throws NoSuchMethodException, IllegalAccessException {
        return lookup.findSpecial(lookup.lookupClass(), methodName, methodType, receiver.getClass()).bindTo(receiver);
    }

    /* TODO Not used yet used
    @SuppressWarnings("unused")
    public static MethodHandle invokeSpecialFromBackOnParameterizedType(Lookup lookup, String method, MethodType type, Object receiver)
            throws NoSuchMethodException, IllegalAccessException {
        Class<?> aClass = receiver.getClass();
        System.out.println("Runtime class : " + aClass);
        return lookup.findSpecial(lookup.lookupClass(), method, type, aClass).bindTo(receiver);
    }
*/

    /**
     * This method calls the constructor <code>backConstructor</code> with the arguments <code>args</code> to instantiate a
     * Back species class instance and then creates an instance of the class <code>frontClass</code> by giving it the
     * Back species instance newly created.
     *
     * @param lookup          the lookup of the enclosing class of the requested invocation
     * @param backConstructor the constructor of the Back species class to invoke
     * @param frontClass      the Front class to instantiate
     * @param args            the arguments to pass during the call of <code>backConstructor</code>
     * @return the newly created Front instance.
     * @throws Throwable
     */
    @SuppressWarnings("unused")
    public static Object createFrontInstance(MethodHandles.Lookup lookup, MethodHandle backConstructor, Class<?> frontClass, Object... args) throws Throwable {
        Object back = backConstructor.asSpreader(Object[].class, args.length).invoke(args);
        return lookup.findConstructor(frontClass, SPECIALIZED_CONSTRUCTOR_TYPE).bindTo(null).bindTo(back).invoke();
    }

    /**
     * Creates a {@link MethodHandle} applying the given {@link MethodHandle resolver} - which will find the good _back__ method -
     * to the arguments starting at the position 0 to return a method handle invoked with the remaining arguments.
     */
    private static MethodHandle invokerOf(MethodType type, MethodHandle resolver) {
        MethodHandle target = MethodHandles.exactInvoker(type);
        return MethodHandles.collectArguments(target, 0, resolver);
    }

    /**
     * This method creates an adapted methodHandle to create an instance of a requested Back species class. If the requested
     * Back species class does not already exist, it is created from the FrontName$Back.class blueprint code, first specialized
     * thanks to the given <code>genericName</code>.
     *
     * @param frontClassLookup the lookup of the Front class
     * @param type             the type of the constructor used to instantiate the Back species class
     * @param frontClass       the Front class
     * @param genericName      the Front class'type variables instantiation. For example Foo<I, Z>
     * @return the methodHandle creating the Back species instance.
     * @throws NoSuchFieldException
     * @throws IllegalAccessException
     * @throws NoSuchMethodException
     * @throws ClassNotFoundException
     */
    private static MethodHandle createBackSpecies(MethodHandles.Lookup frontClassLookup, MethodType type, Class<?> frontClass, String genericName)
            throws NoSuchFieldException, IllegalAccessException, NoSuchMethodException, ClassNotFoundException {
        // TODO BUG : Use a value which is a HashMap storing the String specialization and the corresponding class.
        // TODO store the Substitution table in a couple values for the key class in classValue so it is not read every time. Not needed, because bootstrap are not called too much.
        byte[] backCode = BACK_FACTORY.get(frontClass);
        SubstitutionTable substitutionTable = SubstitutionTableReader.read(backCode);
        String[] classes = Type.getParameterizedTypeValues(genericName);
        Object[] pool = new Object[substitutionTable.getMax() + 1];
        for (Map.Entry<Integer, Map.Entry<String, String>> descs : substitutionTable.getDescriptors().entrySet()) {
            Integer index = descs.getKey();
            Map.Entry<String, String> ownerAndDescriptor = descs.getValue();
            String owner = ownerAndDescriptor.getKey();
            String descriptor = ownerAndDescriptor.getValue();

            if (!owner.equals(BackClassVisitor.RT_METHOD_HANDLE_TYPE)) {
                pool[index] = Type.specializeDescriptor(descriptor, classes);
                continue;
            }
            switch (descriptor) {
                case BackClassVisitor.HANDLE_RT_BSM_NEW:
                    pool[index] = patchMethodHandleInConstantPool(frontClassLookup, "bsm_createAny", TYPE_BSM_CREATE_ANY, 4);
                    break;
                case BackClassVisitor.HANDLE_RT_BSM_GET_FIELD:
                    pool[index] = patchMethodHandleInConstantPool(frontClassLookup, "bsm_getField", TYPE_BSM_GETFIELD, 4);
                    break;
                case BackClassVisitor.HANDLE_RT_BSM_PUT_FIELD:
                    pool[index] = patchMethodHandleInConstantPool(frontClassLookup, "bsm_putField", TYPE_BSM_PUTFIELD, 4);
                    break;
                case BackClassVisitor.HANDLE_RT_BSM_INVOKE_SPECIAL_FROM_BACK:
                    pool[index] = patchMethodHandleInConstantPool(frontClassLookup, "bsm_invokeSpecialFromBack", TYPE_BSM_INVOKE_SPECIAL_FROM_BACK, 4);
                    break;
                case BackClassVisitor.HANDLE_RT_METAFACTORY:
                    pool[index] = patchMethodHandleInConstantPool(frontClassLookup, "metafactory", TYPE_METAFACTORY, 3);
                    break;
            }
        }
        Class<?> backClass = UNSAFE.defineAnonymousClass(Object.class, backCode, pool);
        MethodHandle constructor = frontClassLookup.findConstructor(backClass, type.changeReturnType(void.class));
        return constructor.asType(type.changeReturnType(Object.class)); // The return type is considered as a plain Object.
    }

    /**
     * This method returns the requested methodHandle. The methodHandle created has the given <code>lookup</code> bound.
     *
     * @param lookup         the lookup to perform find and bind to the resulting methodHandle
     * @param methodName     the method's name
     * @param methodType     the method's type
     * @param lookupPosition the lookup's position at which it is inserted
     * @throws NoSuchMethodException
     * @throws IllegalAccessException
     */
    private static MethodHandle patchMethodHandleInConstantPool(Lookup lookup, String methodName, MethodType methodType, int lookupPosition)
            throws NoSuchMethodException, IllegalAccessException {
        MethodHandle mh = lookup.findStatic(RT.class, methodName, methodType);
        return MethodHandles.insertArguments(mh, lookupPosition, lookup).asSpreader(Object[].class, 4).
                asType(MethodType.methodType(Object.class, Object[].class));
    }

    /**
     * Returns the back field of a Front - any - class instance.
     *
     * @param lookup the lookup to find the field's getter
     * @param owner  the field's owner
     * @return the Back species instance found.
     * @throws Throwable
     */
    private static Object getBack__(MethodHandles.Lookup lookup, Object owner) throws Throwable {
        return lookup.findGetter(owner.getClass(), FrontClassVisitor.BACK_FIELD, Object.class).invoke(owner);
    }

    /**
     * Computes the lookup of the given <code>type</code>.
     *
     * @param type the lookup's type class
     * @return the lookup computed.
     * @throws Throwable
     */
    private static Lookup computeLookup(Class<?> type) throws Throwable {
        return (Lookup) LOOKUP_CONSTRUCTOR_MH.invokeExact(type);
    }

    /**
     * Initializes the Unsafe field from the class {@link Unsafe}. It is set accessible.
     *
     * @return the initialized Unsafe field.
     */
    private static Unsafe initUnsafe() {
        try {
            Class<?> unsafeClass = Unsafe.class;
            Field theUnsafe = null;
            theUnsafe = unsafeClass.getDeclaredField("theUnsafe");
            theUnsafe.setAccessible(true);
            return (Unsafe) theUnsafe.get(null);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * This bootstrap method returns an adapted callSite to match the virtual method signature defined
     * in the XYZ$any interface.
     */
    public static CallSite metafactory(MethodHandles.Lookup caller, String invokedName,
                                       MethodType invokedType, MethodHandles.Lookup front, String dummy) throws ReflectiveOperationException {
        // System.out.println("metafactory : caller = [" + caller + "], invokedName = [" + invokedName + "], invokedType = [" + invokedType + "], front = [" + front + "], dummy = [" + dummy + "]");
        List<Class<?>> params = invokedType.parameterList();
        if (params.isEmpty()) {
            throw new AssertionError("Missing dynamic parameters!");
        }
        MethodHandle res = /*front*/caller.findStatic(RT.class, invokedName, invokedType);
        return new ConstantCallSite(res);
    }

    /**
     * equals
     **/

    public static boolean equals(byte b1, byte b2) {
        return b1 == b2;
    }

    public static boolean equals(short s1, short s2) {
        return s1 == s2;
    }

    public static boolean equals(char c1, char c2) {
        return c1 == c2;
    }

    public static boolean equals(int i1, int i2) {
        return i1 == i2;
    }

    public static boolean equals(long l1, long l2) {
        return l1 == l2;
    }

    public static boolean equals(float f1, float f2) {
        return f1 == f2;
    }

    public static boolean equals(double d1, double d2) {
        return d1 == d2;
    }

    public static boolean equals(boolean b1, boolean b2) {
        return b1 == b2;
    }

    public static boolean equals(Object o1, Object o2) {
        return o1.equals(o2);
    }

    /**
     * toString
     **/

    public static String toString(byte b) {
        return String.valueOf(b);
    }

    public static String toString(short s) {
        return String.valueOf(s);
    }

    public static String toString(char c) {
        return String.valueOf(c);
    }

    public static String toString(int i) {
        return String.valueOf(i);
    }

    public static String toString(long l) {
        return String.valueOf(l);
    }

    public static String toString(float f) {
        return String.valueOf(f);
    }

    public static String toString(double d) {
        return String.valueOf(d);
    }

    public static String toString(boolean b) {
        return String.valueOf(b);
    }

    public static String toString(Object o) {
        return o.toString();
    }

    /**
     * hashCode
     */

    public static int hashCode(byte b) {
        return Objects.hashCode(b);
    }

    public static int hashCode(short s) {
        return Objects.hashCode(s);
    }

    public static int hashCode(char c) {
        return Objects.hashCode(c);
    }

    public static int hashCode(int i) {
        return Objects.hashCode(i);
    }

    public static int hashCode(long l) {
        return Objects.hashCode(l);
    }

    public static int hashCode(float f) {
        return Objects.hashCode(f);
    }

    public static int hashCode(double d) {
        return Objects.hashCode(d);
    }

    public static int hashCode(boolean b) {
        return Objects.hashCode(b);
    }

    public static int hashCode(Object o) {
        return o.hashCode();
    }
}
