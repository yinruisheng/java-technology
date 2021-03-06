# Java 并发学习记录之 Lock 锁

[TOC]

## Lock 接口

锁是用于通过多个线程控制对共享资源的访问的工具。一般来说，锁提供对共享资源的独占访问：一次只能有一个线程可以获取锁，并且对共享资源的所有访问都要求首先获取锁。 但是，一些锁可能允许并发访问共享资源，如ReadWriteLock的读写锁。

在Lock接口出现之前，Java程序是靠synchronized关键字实现锁功能的。JDK1.5之后并发包中新增了Lock接口以及相关实现类来实现锁功能。虽然缺少了隐式获取释放锁的便捷性，但是却拥有了锁获取与释放的可操作性、可中断的获取锁以及超时获取锁等多种 synchronized 关键字所不具备的同步特性。

### 简单用法

```java
Lock lock=new ReentrantLock();
lock.lock();
try{
  
} finally {
  lock.unlock();
}
```

因为 Lock 是接口所以使用时要结合它的实现类，另外在 finall 语句块中释放锁的目的是保证获取到锁之后，最终能够被释放。同时注意最好不要把获取锁的过程写在 try 语句块中，因为如果在获取锁时发生了异常，异常抛出的同时也会导致锁无故释放。

### Lock接口提供的synchronized关键字不具备的主要特性：

| 特性 | 描述 |
| --- | --- |
| 尝试非阻塞地获取锁 | 当前线程尝试获取锁，如果这一时刻锁没有被其他线程获取到，则成功获取并持有锁 |
| 能被中断地获取锁 | 获取到锁的线程能够响应中断，当获取到锁的线程被中断时，中断异常将会被抛出，同时锁会被释放 |
| 超时获取锁 | 在指定的截止时间之前获取锁， 超过截止时间后仍旧无法获取则返回 |

### Lock接口基本的方法：

| 方法名称 | 描述 |
| --- | --- |
| void lock() | 获得锁。如果锁不可用，则当前线程将被禁用以进行线程调度，并处于休眠状态，直到获取锁。 |
| void lockInterruptibly() | 获取锁，如果可用并立即返回。如果锁不可用，那么当前线程将被禁用以进行线程调度，并且处于休眠状态，和lock()方法不同的是在锁的获取中可以中断当前线程（相应中断）。 |
| Condition newCondition() | 获取等待通知组件，该组件和当前的锁绑定，当前线程只有获得了锁，才能调用该组件的wait()方法，而调用后，当前线程将释放锁。 |
| boolean tryLock() | 只有在调用时才可以获得锁。如果可用，则获取锁定，并立即返回值为true；如果锁不可用，则此方法将立即返回值为false 。 |
| boolean tryLock(long time, TimeUnit unit) | 超时获取锁，当前线程在一下三种情况下会返回：<br/>1. 当前线程在超时时间内获得了锁；<br/>2.当前线程在超时时间内被中断；<br/>3.超时时间结束，返回false. |
| void unlock() | 释放锁。 |

### 对同步器 AbstractQueuedSynchronizer 的分析

- [ ] Java并发学习记录之同步器AbstractQueuedSynchronizer

## Lock 实现类：ReentrantLock

重入锁 ReentrantLock，顾名思义，就是支持重进入的锁，它表示该锁能够支持一个线程对资源的重复加锁。除此之外，还支持获取锁时的公平和非公平锁选择。

### 加锁2次释放2次

```java
package com.littlefxc.examples.base.thread.lock;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author fengxuechao
 * @date 2019/2/28
 **/
public class ReentrantLockR implements Runnable {

    Lock lock = new ReentrantLock();

    @Override
    public void run() {
        lock.lock();
        System.out.println("线程 " + Thread.currentThread().getId() + " 加锁");
        try {
            lock.lock();// 重进入验证
            for (int i = 0; i < 5; i++) {
                System.out.println("线程 " + Thread.currentThread().getId() + " 循环计数 = " + i);
            }
        } finally {
            lock.unlock();// 锁的释放
            System.out.println("线程 " + Thread.currentThread().getId() + " 解锁");
            lock.unlock();
        }
    }

    public static void main(String[] args) {
        ReentrantLockR lockR = new ReentrantLockR();
        Thread t1 = new Thread(lockR);
        Thread t2 = new Thread(lockR);
        Thread t3 = new Thread(lockR);
        t1.start();
        t2.start();
        t3.start();
    }
}
```

运行结果：

![ReentrantLockR运行结果](../images/ReentrantLockR运行结果.png)

可以看到，运行结果是只有当线程运行完毕后才会释放锁，其它线程才能获得锁->执行业务代码->释放锁。**其他线程的执行顺序是不确定的**（为了这个图运行了很多遍）。

仔细观察代码，可以我加了两遍锁，同时也释放了两遍锁。

### 如果，加锁两次释放一次：

```java
@Override
public void run() {
    lock.lock();
    lock.lock();// 重进入验证
    System.out.println("线程 " + Thread.currentThread().getId() + " 加锁");
    try {
        for (int i = 0; i < 5; i++) {
            System.out.println("线程 " + Thread.currentThread().getId() + " 循环计数 = " + i);
        }
    } finally {
//      lock.unlock();// 锁的释放
        System.out.println("线程 " + Thread.currentThread().getId() + " 解锁");
        lock.unlock();
    }
}
```

运行结果：

![ReentrantLockR运行结果没有完整释放锁](../images/ReentrantLockR运行结果没有完整释放锁.png)

### 又如果，加锁1次却释放锁两次，就会抛异常：

```java
@Override
public void run() {
    lock.lock();
    //lock.lock();// 重进入验证
    System.out.println("线程 " + Thread.currentThread().getId() + " 加锁");
    try {
        for (int i = 0; i < 5; i++) {
            System.out.println("线程 " + Thread.currentThread().getId() + " 循环计数 = " + i);
        }
    } finally {
        lock.unlock();// 锁的释放
        System.out.println("线程 " + Thread.currentThread().getId() + " 解锁");
        lock.unlock();
    }
}
```

运行结果：

![ReentrantLockR运行结果_加锁1次释放2次](../images/ReentrantLockR运行结果_加锁1次释放2次.png)

### 公平与非公平获取锁的区别

如果一个锁是公平的，那么锁的获取顺序就应该符合请求的绝对时间顺序，也就是 FIFO。

ReentrantLock 的 非公平锁的获取（源码）：

```java
final boolean nonfairTryAcquire(int acquires) {
    final Thread current = Thread.currentThread();
    int c = getState();
    if (c == 0) {
        if (compareAndSetState(0, acquires)) {
            setExclusiveOwnerThread(current);
            return true;
        }
    }
    else if (current == getExclusiveOwnerThread()) {
        int nextc = c + acquires;
        if (nextc < 0) // overflow
            throw new Error("Maximum lock count exceeded");
        setState(nextc);
        return true;
    }
    return false;
}
```

ReentrantLock 的 公平锁的获取（源码）：

```java
/**
 * Fair version of tryAcquire.  Don't grant access unless
 * recursive call or no waiters or is first.
 */
protected final boolean tryAcquire(int acquires) {
    final Thread current = Thread.currentThread();
    int c = getState();
    if (c == 0) {
        if (!hasQueuedPredecessors() &&
            compareAndSetState(0, acquires)) {
            setExclusiveOwnerThread(current);
            return true;
        }
    }
    else if (current == getExclusiveOwnerThread()) {
        int nextc = c + acquires;
        if (nextc < 0)
            throw new Error("Maximum lock count exceeded");
        setState(nextc);
        return true;
    }
    return false;
}
```

对比两个方法的不同点只是获取公平锁判断条件多了 `hasQueuedPredecessors()` 方法，即加入了同步队列中当前是否有前驱结点的判断，如果该方法返回 true，则表示当前线程需要等待前驱线程获取锁并释放锁之后才能继续获取锁。

当你看到 ReentrantLock 的构造函数，为什么非公平锁会被设定为默认的实现呢？

要知道一个刚释放锁的线程再次获取锁的几率会非常大，使得其他线程只能在同步队列中等待，从而让非公平锁使线程“饥饿”， 这是因为虽然公平锁保证了锁的获取按照 FIFO 原则，但代价是进行了大量的线程切换。非公平锁虽然可能会造成线程“饥饿”，但极少的线程切换，保证了更大的吞吐量。

## 读写锁：ReadWriteLock

之前学习过的如 `synchronized` 关键字、`ReentrantLock` 重入锁都是排他锁，这些锁在同一时刻只允许一个线程进行访问。读写锁不同，在同一时刻允许多个**读线程**访问，但是在**写线程**访问时，所有的读线程和其它线程均会被阻塞。读写锁维护了一对锁（读锁->共享锁和写锁->排他锁），通过分离读锁和写锁，使得并发性相比一般的排他锁有了很大的提升。

假设有这样一种情况，在程序中定义了一个共享的缓存数据结构，它在大部分的时间读服务（如查询）使用的很多，而写的服务很少，但每次写完后数据对读的服务可见。（读：很多；写：很少）

在这种情况下，你很可能会使用等待/通知机制来实现从而保证数据不会出现脏读。

改用读写锁来实现的话，只需在读操作时获取读锁，写操作时获取写锁。当前线程进行写操作时，其它的读写线程阻塞，当档期那线程的写锁释放后，其它线程继续执行，而如果其它线程都是读线程，那么都允许执行。

也就是说，在读大于写的情况下，使用读写锁具有比其他排他锁更好的并发性和吞吐量。

### ReadWriteLock 的实现类 ReentrantReadWriteLock 的特性

| 特性       | 说明                                                                                                                             |
|----------| ---------- |
| 公平性选择 | 支持非公平（默认）和公平的锁获取方式，吞吐量上来看还是非公平优于公平 |
| 重进入     | 该锁支持重进入，以读写线程为例：读线程在获取了读锁之后，能够再次获取读锁。而写线程在获取了写锁之后能够再次获取写锁也能够同时获取读锁 |
| 锁降级     | 遵循获取写锁、获取读锁再释放写锁的次序，写锁能够降级称为读锁 |

### 读写锁 ReentrantReadWriteLock 的使用测试

```java
package com.littlefxc.examples.base.thread.lock;

import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * @author fengxuechao
 * @date 2019/3/1
 **/
public class ReentrantReadWriteLockTest {

    private ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    public void read() {
        try {
            try {
                lock.readLock().lock();
                System.out.println("线程 " + Thread.currentThread().getId() + " 获得读锁 " + System.currentTimeMillis());
                Thread.sleep(10000);
            } finally {
                lock.readLock().unlock();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void write() {
        try {
            try {
                lock.writeLock().lock();
                System.out.println("线程 " + Thread.currentThread().getId() + " 获得写锁 " + System.currentTimeMillis());
                Thread.sleep(10000);
            } finally {
                lock.writeLock().unlock();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
```

上述代码中，read() 方法中是关于读锁的操作， write() 方法中是关于写锁的操作。为了加强效果，分别让两个操作都睡眠 10s。

接下来将使用这两个方法进行组合从而进行三方面的测试：测试多个读锁间的不互斥、测试读锁与写锁互斥、测试写锁与写锁互斥。

#### 测试多个读锁间的不互斥

```java
public static void main(String[] args) {
    ReentrantReadWriteLockTest rwlSample = new ReentrantReadWriteLockTest();
    Thread t1 = new Thread(() -> rwlSample.read());
    Thread t2 = new Thread(() -> rwlSample.read());
    t1.start();
    t2.start();
}
```

运行后，发现两个线程几乎同时获得读锁。

#### 测试读锁与写锁互斥

```java
public static void main(String[] args) {
    ReentrantReadWriteLockTest rwlSample = new ReentrantReadWriteLockTest();
    Thread t1 = new Thread(() -> rwlSample.read());
    Thread t2 = new Thread(() -> rwlSample.write());
    t1.start();
    t2.start();
}
```

运行后，发现两个线程互斥。

#### 测试写锁与写锁互斥

```java
public static void main(String[] args) {
    ReentrantReadWriteLockTest rwlSample = new ReentrantReadWriteLockTest();
    Thread t1 = new Thread(() -> rwlSample.write());
    Thread t2 = new Thread(() -> rwlSample.write());
    t1.start();
    t2.start();
}
```

运行后，发现两个线程互斥。

### 对读写锁的分析

- [ ] Java并发学习记录之读写锁分析

## Condition 接口

`synchronized` 关键字与 `wait()` 和 `notify/notifyAll()` 方法相结合可以实现等待/通知机制，
`Lock` 接口同样定义了等待/通知两种类型的方法，和前者一样，当前线程在调用这些方法前，
需要提前获取到 `Condition` 对象关联的锁。`Condition` 对象是由 `Lock` 接口实现类创建出来的 
`Condition condition = lock.newCondition();`。

在使用 notify/notifyAll()方法进行通知时，被通知的线程是有JVM选择的，
使用 Lock 类结合 Condition 实例可以实现“选择性通知”，这个功能非常重要，
而且是 Condition 接口默认提供的。

但是 synchronized 关键字就相当于整个 Lock 对象中只有一个 Condition 实例，
所有的线程都注册在它一个身上。如果执行 `notifyAll()` 方法的话就会通知所有处于等待状态的线程这样会造成很大的效率问题，
而 Condition 实例的 `signalAll()` 方法只会唤醒注册在该 Condition 实例中的所有等待线程。

Condition 定义的（部分）方法介绍：

| 方法名称       | 说明 |
|----------| ---------- |
| void await() throws InterruptedException | 当前线程进入等待状态直到被通知或中断，当前线程将进入运行状态且从 await() 方法返回的情况，包括：<br/> 其它线程调用该 Condition 的 signal() 或 signalAll() 方法，而当前线程被选中唤醒<ul><li>其它线程（调用 interrupt() 方法）中断当前线程</li><li>如果当前等待线程从 await() 方法返回，那么表明该线程已经获取了 Condition 对象所对应的锁</li></ul> |
| void awaitUninterruptibly() | 当前线程进入等待状态直到被通知，对中断不敏感 |
| long awaitNanos(long nanosTimeout) throws InterruptedException | 当前线程进入等待状态直到被通知、中断或者到某个时间。返回值表示剩余时间，如果返回值是 0 或者负数，就表示超时 |
| long awaitNanos(long nanosTimeout) throws InterruptedException | 当前线程进入等待状态直到被通知、中断或者到某个时间。如果没有到指定时间就被通知返回 true，否则返回 false |
| boolean awaitUntil(Date deadline) throws InterruptedException | 遵循获取写锁、获取读锁再释放写锁的次序，写锁能够降级称为读锁 |
| void signal() | 唤醒一个等待在 Condition 实例上的线程 |
| void signalAll() | 唤醒所有等待在 Condition 实例上的线程 |

### Condition 使用示例

```java
package com.littlefxc.examples.base.thread.lock;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author fengxuechao
 * @date 2019-03-05
 */
public class ConditionSample {

    Lock lock = new ReentrantLock();

    Condition condition = lock.newCondition();

    public void conditionWait() {
        lock.lock();
        try {
            System.out.println("线程 " + Thread.currentThread().getId() + " 释放锁并开始等待");
            long l = System.currentTimeMillis();
            condition.await();
            long time = System.currentTimeMillis() - l;
            System.out.println("线程 " + Thread.currentThread().getId() + " 获得锁并结束等待, 等待时间是 " + time + "ms");
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            lock.unlock();
        }
    }

    public void conditionSignal() {
        lock.lock();
        try {
            System.out.println("线程 " + Thread.currentThread().getId() + " 开始释放锁并通知线程等待队列");
            condition.signal();
            Thread.sleep(2000);
            System.out.println("线程 " + Thread.currentThread().getId() + " 释放锁并通知线程等待队列");
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            lock.unlock();
        }
    }

    public static void main(String[] args) {
        ConditionSample sample = new ConditionSample();
        Thread t1 = new Thread(() -> sample.conditionWait());
        Thread t2 = new Thread(() -> sample.conditionSignal());
        t1.start();
        t2.start();
    }

}
```

运行结果：

![ConditionSample运行结果](../images/ConditionSample运行结果.png)

在使用 wait/notify 实现等待通知机制的时候我们知道必须执行完 notify() 方法所在的 synchronized 代码块后才释放锁。
在这里也一样，必须执行完 signal 所在的 try 语句块之后才释放锁，condition.await() 后的语句才能被执行。

## 参考：

《Java并发编程的艺术》
《Java并发编程实战》