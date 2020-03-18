#### Hibernate实现原理

Hibernate是怎样实现呢？主要是依据反射机制。

   现在以一次数据库查询操作分析Hibernate实现原理。

   假设有一个用户表(tbl_user),表中字段有id,name,sex。同时有一个实体类(User)与其相对应，查询语句是:  select * from User。

1.在项目启动时，Hibernate配置文件中的内容已经存储在容器中,存储着表与实体中的关系。

2.在执行select * from User 时，会根据反射机制先找到User的全路径名称，进而找到容器中User对应的配置。

3.由于配置文件中的实体属性与数据库中的字段是对应的，Hibernate会将select * from User 这个hql语句根据不同的数据库方言解析成不同的SQL语句(select * from tbl_user)。

   大致过程就是这样，当然，器内部实现的具体过程是比较复杂的,在使用Hibernate进行数据库操作时，应注意级联、延迟加载、缓存的使用。

​      

简单来说就是，**利用反射原理，将实体类中的字段按照xml配置或者annotation解析成一条或者多条sql语句，然后放入数据库执行**，说的简单点，就是这么个原理，但是内部实现比较复杂   

 

 

原理：

1.通过Configuration().configure();读取并解析hibernate.cfg.xml配置文件

2.由hibernate.cfg.xml中的<mapping resource="com/xx/User.hbm.xml"/>读取并解析映射信息

3.通过config.buildSessionFactory();//创建SessionFactory

4.sessionFactory.openSession();//打开Sesssion

5.session.beginTransaction();//创建事务Transation

6.persistent operate持久化操作

7.session.getTransaction().commit();//提交事务

8.关闭Session

9.关闭SesstionFactory

 

这个就是原理，不是流程。主要就是一个**基于JDBC的主流持久化框架**，一个优秀的ORM实现，**对JDBC访问数据库的代码做了封装**，很大程度上监护了DAO层的编码工作。

 

**hibernate缺点：**

总的来说，hibernate的缺点主要有以下几点：

一、由于对持久层封装过于完整，导致开发人员无法对SQL进行优化，**无法灵活使用JDBC的原生SQL**，Hibernate封装了JDBC，所以没有JDBC直接访问数据库效率高。要使用数据库的特定优化机制的时候，不适合用Hibernate 

二、框架中使用ORM原则，导致配置过于复杂，一旦遇到大型项目，比如300张表以上，配置文件和内容是非常庞大的，另外，DTO满天飞，性能和维护问题随之而来

三、如果项目中各个表中关系复杂，**表之间的关系很多，在很多地方把lazy都设置false，会导致数据查询和加载很慢**，尤其是级联查询的时候。

四、Hibernate在批量数据处理时有弱势，对于**批量的修改，删除，不适合用Hibernate**,这也是ORM框架的弱点

 

 

Hibernate是ORM框架（object-relation maping对象关系映射），它是用来实现JDBC的功能,但是它不能替换JDBC,它是在JDBC基础上实现的，即Hibernate中已经把JDBC封装了，最终的代码是到HIbernate在传递到JDBC在于数据库交换，所以性能没有JDBC直接与数据库交互快

 