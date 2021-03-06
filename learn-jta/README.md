# 分布式事务原理探究

## 概念梳理

首先，我在学习分布式事务的过程中，遇到了许多概念，例如：X/Open XA、两段式提交（2PC）、JTA等。
概念有点多，需要记录梳理以下。

### X/Open XA

> (摘自[维基百科](https://zh.wikipedia.org/wiki/X/Open_XA))在计算技术上，
> XA规范是开放群组关于分布式事务处理 (DTP)的规范。
> 规范描述了全局的事务管理器与局部的资源管理器之间的接口。
> XA规范的目的是允许多个资源（如数据库，应用服务器，消息队列，等等）
> 在同一事务中访问，这样可以使ACID属性跨越应用程序而保持有效。
> XA使用两阶段提交来保证所有资源同时提交或回滚任何特定的事务。
> XA规范描述了资源管理器要支持事务性访问所必需做的事情。
> 遵守该规范的资源管理器被称为XA compliant。

### DTP

DTP(全称Distributed Transaction Processing Reference Model)，也是一个规范，
他定义了一些模型对象和对象间行为，通过这些对象和对象间行为来指导分布式事务实现。

![DTP.png](images/DTP.png)

由上图可知，分布式事务有三个对象参与完成，他们分别是：

- 应用程序（application program）（AP）：定义了事务边界（定义事务开始和结束）
并指定构成事务的操作。可以将AP理解为service层接口，
三层开发原则告诉我们需要在service层开启事务。
- 资源管理器（Resource Manager）（RM）：顾名思义，
资源管理器用来管理我们需要访问的共享资源，
我们可以将它理解为关系数据库、文件存储系统、消息队列等，资源包含比如数据库、
文件系统、打印机服务器等。
- 事务管理器（transaction manager）（TM）：负责管理全局事务，分配事务唯一标识，
监控事务的执行进度，并负责事务的提交、回滚、失败恢复等。

### XA

了解了DTP的定义，我们再看XA规范，XA规范描述全局的事务管理器（TM）
与局部的资源管理器(RM)之间的接口，这个接口不是编程层面的接口，
而是DTO定义模型之间直接交互的系统接口。

我常常看到某某数据库支持 XA 协议，某某分布式事务中间件支持XA协议，
现在我理解它们的含义了，因为分布式事务参与者众多，
只有大家都遵循同一个规则交互，才能最终完成分布式事务。

### 两阶段式提交

> (摘自[维基百科](https://zh.wikipedia.org/wiki/%E4%BA%8C%E9%98%B6%E6%AE%B5%E6%8F%90%E4%BA%A4))
> 二阶段提交（英语：Two-phase Commit）是指在计算机网络以及数据库领域内，
> 为了使基于分布式系统架构下的所有节点在进行事务提交时保持一致性而设计的一种算法。
> 通常，二阶段提交也被称为是一种协议（Protocol）。在分布式系统中，
> 每个节点虽然可以知晓自己的操作时成功或者失败，却无法知道其他节点的操作的成功或失败。
> 当一个事务跨越多个节点时，为了保持事务的ACID特性，
> 需要引入一个作为协调者的组件来统一掌控所有节点（称作参与者）
> 的操作结果并最终指示这些节点是否要把操作结果进行真正的提交
> （比如将更新后的数据写入磁盘等等）。因此，二阶段提交的算法思路可以概括为： 
> 参与者将操作成败通知协调者，
> 再由协调者根据所有参与者的反馈情报决定各参与者是否要提交操作还是中止操作。
> 需要注意的是，二阶段提交（英语：2PC）不应该与并发控制中的二阶段锁（英语：2PL）混淆。

简单来说，如果两阶段式提交是XA协议的一种实现算法。

所有实现XA协议的关系数据库和第三方分布式事务中间件都使用2PC算法。

### JTA

> (摘自[维基百科](https://zh.wikipedia.org/wiki/Java%E4%BA%8B%E5%8A%A1API))
> Java事务API（Java Transaction API，简称JTA ） 是一个Java企业版 的应用程序接口，
> 在Java环境中，允许完成跨越多个XA资源的分布式事务。
> JTA是在Java社区过程下制定的规范，编号JSR 907。JTA提供：
> - 划分事务边界
> - X/Open XA API允许资源参与到事务中。

简单来说，JTA 是用 JAVA 实现的 XA 协议，但是他只定义了相应的接口，并没有给出具体实现，
为了能够完成分布式事务，我们需要自己按照“两段式提交”算法完成自己的实现，
或者借助第三方中间件，例如BTM，atomikos等。

## 总结

1. DTP定义分布式事务过程。
2. XA定义DTP中TM和RM的交互行为。
3. XA由2PC算法实现。
4. JTA主要是XA规范的JAVA接口描述

