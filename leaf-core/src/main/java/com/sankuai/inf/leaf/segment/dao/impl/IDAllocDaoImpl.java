package com.sankuai.inf.leaf.segment.dao.impl;

import com.sankuai.inf.leaf.segment.dao.IDAllocDao;
import com.sankuai.inf.leaf.segment.dao.IDAllocMapper;
import com.sankuai.inf.leaf.segment.model.LeafAlloc;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.TransactionFactory;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;

import javax.sql.DataSource;
import java.util.List;

/**
 *
 * @author mickle
 */
public class IDAllocDaoImpl implements IDAllocDao {

    SqlSessionFactory sqlSessionFactory;

    public IDAllocDaoImpl(DataSource dataSource) {
        TransactionFactory transactionFactory = new JdbcTransactionFactory();
        Environment environment = new Environment("development", transactionFactory, dataSource);
        Configuration configuration = new Configuration(environment);
        configuration.addMapper(IDAllocMapper.class);
        sqlSessionFactory = new SqlSessionFactoryBuilder().build(configuration);
    }

    @Override
    public List<LeafAlloc> getAllLeafAllocs() {
      try (SqlSession sqlSession = sqlSessionFactory.openSession(false)) {
        IDAllocMapper mapper = sqlSession.getMapper(IDAllocMapper.class);
        return mapper.getAllLeafAllocs();
      }
    }

    @Override
    public LeafAlloc updateMaxIdAndGetLeafAlloc(String tag) {
      try (SqlSession sqlSession = sqlSessionFactory.openSession()) {
        IDAllocMapper mapper = sqlSession.getMapper(IDAllocMapper.class);
        mapper.updateMaxId(tag);
        sqlSession.commit();
        return mapper.getLeafAlloc(tag);
      }
    }

    @Override
    public LeafAlloc updateMaxIdByCustomStepAndGetLeafAlloc(LeafAlloc leafAlloc) {
      try (SqlSession sqlSession = sqlSessionFactory.openSession()) {
        IDAllocMapper mapper = sqlSession.getMapper(IDAllocMapper.class);
        mapper.updateMaxIdByCustomStep(leafAlloc);
        LeafAlloc result = mapper.getLeafAlloc(leafAlloc.getKey());
        sqlSession.commit();
        return result;
      }
    }

    @Override
    public List<String> getAllTags() {
      try (SqlSession sqlSession = sqlSessionFactory.openSession(false)) {
        IDAllocMapper mapper = sqlSession.getMapper(IDAllocMapper.class);
        return mapper.getAllTags();
      }
    }
}
