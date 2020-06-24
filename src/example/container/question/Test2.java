package example.container.question;

/**
 * Test2：ArrayList既然继承自AbstractList抽象类，而AbstractList已经实现了List接口，
 *        那么ArrayList类为何还要再实现List接口呢？
 * 运行结果：About to call foo() on example.container.Test1$Class2@2c7b84de
 *         BaseClass.foo
 *         Class2.foo
 *         Exception in thread "main" java.lang.ClassCastException: com.sun.proxy.$Proxy1 cannot be cast to example.container.Test1$MyInterface
 * 	       at example.container.Test2.main(Test2.java:28)
 * @Description 虽然Class1确实隐式实现了MyInterface，但是创建的代理却没有。
 * 因此，如果我们要创建一个动态代理来为具有隐式接口继承的对象实现所有接口，那么一般的唯一方法是将超类一直返回到java.lang.Object，并进行遍历。
 * 所有实现的接口及其超类（请记住Java支持多接口继承），听起来效率不高，而显式命名接口要容易得多（且更快），因为我认为它们是在编译时设置的。
 * @author: yatao.ma
 * @date: 2020/6/24 4:02 下午
 */
import example.container.question.Test1;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public class Test2 extends Test1 {

 public static void main(String[] args) {

  MyInterface c1 = new Class1();
  MyInterface c2 = new Class2();

  // Note the order...
  MyInterface proxy2 = createProxy(c2);
  proxy2.foo();

  // This fails with an unchecked exception
  MyInterface proxy1 = createProxy(c1);
  proxy1.foo();
 }

 private static <T> T createProxy(final T obj) {

  final InvocationHandler handler = new InvocationHandler() {

   @Override
   public Object invoke(Object proxy, Method method, Object[] args)
     throws Throwable {
    System.out.printf("About to call %s() on %s\n", method
      .getName(), obj);
    return method.invoke(obj, args);
   }
  };

  return (T) Proxy.newProxyInstance(obj.getClass().getClassLoader(), obj
    .getClass().getInterfaces(), handler);
   }
}
