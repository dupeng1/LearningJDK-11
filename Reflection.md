//直接通过类的静态变量来获取
Class<Integer> intClass = Integer.class;

//通过实例变量的getClass方法
Integer integer = new Integer(0);
Class<? extends Integer> aClass = integer.getClass();

//通过Class.forName("类的全限定名")
Class<?> aClass1 = Class.forName("java.lang.Integer");

上述三种就是获取某个Class的class实例的方式，需要注意的是，JVM只会加载一个Class实例，也就是说上述三种方式获取到的class实例都是一样的。

2、
Java反射机制通过java.lang.reflect.Array类来创建数组。下面是一个如何创建数组的例子：
int[] intArray = (int[]) Array.newInstance(int.class, 3);
这个例子创建一个int类型的数组。Array.newInstance()方法的第一个参数表示了我们要创建一个什么类型的数组。
第二个参数表示了这个数组的空间是多大。

通过Java反射机制同样可以访问数组中的元素。具体可以使用Array.get(…)和Array.set(…)方法来访问。

3、
类型变量的声明：<E>，前后需加上尖括号

//1.在类上声明类型变量
class A<T>{
T a;
}//之后这里可用任意类型替换T，例如
A<String> as = new A<String>();
//是否看着有点像集合？不错，集合就是泛型的一个典型运用

//2.在方法上声明
public <E> void test(E e){}
//方法上，类型变量声明（定义）不是在参数里边，而且必须在返回值之前,static等修饰后

//3.在构造器上声明
public <K> A(K k){}






