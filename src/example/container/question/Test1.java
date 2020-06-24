package example.container.question;

import java.io.Serializable;
import java.util.Arrays;

/**
 * Test1：ArrayList既然继承自AbstractList抽象类，而AbstractList已经实现了List接口，
 *        那么ArrayList类为何还要再实现List接口呢？
 * 运行结果： class example.container.Test1$BaseClass --> [interface example.container.Test1$MyInterface, interface java.lang.Cloneable, interface java.io.Serializable]
 * class example.container.Test1$Class1 --> []
 * class example.container.Test1$Class2 --> [interface example.container.Test1$MyInterface, interface java.lang.Cloneable, interface java.io.Serializable]
 * @Description: Class1没有定义显式接口，因此Class＃getInterfaces（）不包括那些接口，
 *                而Class2包括这些接口。仅在此程序中可以清楚地使用它：
 * @author: yatao.ma
 * @date: 2020/6/24 3:53 下午
 */
public class Test1 {

    public static interface MyInterface {
        void foo();
    }

    public static class BaseClass implements MyInterface, Cloneable, Serializable {

        @Override
        public void foo() {
            System.out.println("BaseClass.foo");
        }
    }

    public static class Class1 extends BaseClass {

        @Override
        public void foo() {
            super.foo();
            System.out.println("Class1.foo");
        }
    }

    static class Class2 extends BaseClass implements MyInterface, Cloneable,
            Serializable {

        @Override
        public void foo() {
            super.foo();
            System.out.println("Class2.foo");
        }
    }

    public static void main(String[] args) {

        showInterfacesFor(BaseClass.class);
        showInterfacesFor(Class1.class);
        showInterfacesFor(Class2.class);
    }

    private static void showInterfacesFor(Class<?> clazz) {
        System.out.printf("%s --> %s\n", clazz, Arrays.toString(clazz
                .getInterfaces()));
    }
}
