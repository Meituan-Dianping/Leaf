package com.sankuai.inf.leaf.segment.dao.impl;

import com.alibaba.druid.pool.DruidDataSource;
import com.sankuai.inf.leaf.segment.dao.DailyIDAllocDao;
import com.sankuai.inf.leaf.segment.dao.DailyIDAllocMapper;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.TransactionFactory;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;

import java.util.List;

/***
 *
 *
 * @author <a href="mailto:1934849492@qq.com">Darian</a> 
 * @date 2021/7/1  23:04
 */
public class DailyIDAllocDaoImpl implements DailyIDAllocDao {

    SqlSessionFactory sqlSessionFactory;


    public DailyIDAllocDaoImpl(DruidDataSource dataSource) {
        TransactionFactory transactionFactory = new JdbcTransactionFactory();
        Environment environment = new Environment("development", transactionFactory, dataSource);
        Configuration configuration = new Configuration(environment);
//        configuration.addMapper(IDAllocMapper.class);
        configuration.addMapper(DailyIDAllocMapper.class);
        sqlSessionFactory = new SqlSessionFactoryBuilder().build(configuration);
    }

    @Override
    public List<String> getAllTags() {
        SqlSession sqlSession = sqlSessionFactory.openSession(false);
        try {
            return sqlSession.selectList("com.sankuai.inf.leaf.segment.dao.DailyIDAllocMapper.getAllTags");
        } finally {
            sqlSession.close();
        }
    }
}
