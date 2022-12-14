```java
/**
 * EvaluationContext
 * EvaluationContext接口用于计算表达式时解析属性、方法或字段，并帮助执行类型转换。
 * SimpleEvaluationContext: 提供基本的SpEL语言特性和配置选项的子集 ,主要是面向各种不需要全spel语法的表达式.
 * SimpleEvaluationContext设计目的就是用来支持spel语法的一个子集. 它不包含java类型引用, 构造函数, bean引用.它还要求使用者准确选择属性和方法在表达式中
 * 支持的程度.默认的`create()`这个静态工厂方法对于属性来说是只读的.可以获得一个构建器来配置所需的精确支持, 比如:
 * 1. 自定义PropertyAccessor (非反射)只访问属性
 * 2. 数据绑定属性, 只读
 * 3. 数据绑定属性, 读写
 * StandardEvaluationContext: 提供了spel全部语言特性和配置, 可以使用这歌类来指定一个默认的 root对象, 并管理各种计算表达式相关的策略
 */
```

类型转换:

默认的, spel使用的是core包下, conversionService类进行类型转换.conversionService提供了许多内置类型转换器, 除此之外, `conversionService`是支持扩展的, 可以自定义类型转换的方式.另外, `spel`支持泛型, 如果再表达式中使用泛型, 也就是说`spel`会通过类型转换来维持表达式中出现的对象类型是正确的.

这在实践中意味着什么? 假设使用setValue()来设置一个List属性。 属性的类型实际上是List<Boolean>。 `SpEL`会识别出来，列表中的元素在放入之前需要转换为布尔值。  

解析设置:

`SpelParserConfiguration`这个类是用来配置`spel表达式解析器`的.该类可以控制某些表达式组件的行为.比如: 当在数组中查找时, 目标下标上元素值为null, `spel`会自动根据元素类型创建元素. 这在使用由属性引用链组成的表达式时非常有用。 如果您在数组或列表中建立索引，并指定超出数组或列表当前大小的索引，那么SpEL可以自动增长数组或列表以适应该索引。 为了在指定的索引处添加元素，在设置指定值之前，SpEL将尝试使用元素类型的默认构造函数创建元素。 如果元素类型没有默认构造函数，则将向数组或列表添加null。 如果没有内置或自定义转换器知道如何设置该值，则null将保留在指定索引处的数组或列表中。