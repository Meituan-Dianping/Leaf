package com.sankuai.inf.leaf.segment.dao.impl;

import com.sankuai.inf.leaf.segment.dao.WorkerIdAllocDao;
import com.sankuai.inf.leaf.segment.dao.WorkerIdAllocMapper;
import com.sankuai.inf.leaf.segment.model.LeafWorkerIdAlloc;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.TransactionFactory;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;

import javax.sql.DataSource;
import java.util.Date;
import java.util.List;

public class WorkerIdAllocDaoImpl implements WorkerIdAllocDao {

    SqlSessionFactory sqlSessionFactory;

    public WorkerIdAllocDaoImpl(DataSource dataSource) {
        TransactionFactory transactionFactory = new JdbcTransactionFactory();
        Environment environment = new Environment("development", transactionFactory, dataSource);
        Configuration configuration = new Configuration(environment);
        configuration.addMapper(WorkerIdAllocMapper.class);
        sqlSessionFactory = new SqlSessionFactoryBuilder().build(configuration);
    }

    @Override
    public LeafWorkerIdAlloc getOrCreateLeafWorkerId(String ip, String port, String ipPort) {
        SqlSession sqlSession = sqlSessionFactory.openSession(false);
        try {
            LeafWorkerIdAlloc workerIdAlloc = new LeafWorkerIdAlloc();
            List<LeafWorkerIdAlloc> workIdAllocList = sqlSession.selectList("com.sankuai.inf.leaf.segment.dao.WorkerIdAllocMapper.getLeafWorkerIdAlloc", ipPort);
            if (workIdAllocList == null || workIdAllocList.size()==0)  {
                workerIdAlloc.setIp(ip);
                workerIdAlloc.setPort(port);
                workerIdAlloc.setIpPort(ipPort);
                workerIdAlloc.setMaxTimestamp(System.currentTimeMillis());
                workerIdAlloc.setCreateTime(new Date());
                workerIdAlloc.setUpdateTime(new Date());
                int status = sqlSession.insert("com.sankuai.inf.leaf.segment.dao.WorkerIdAllocMapper.insertIfNotExist", workerIdAlloc);
            } else {
                workerIdAlloc = workIdAllocList.get(0);
            }
            sqlSession.commit();
            return workerIdAlloc;
        } finally {
            sqlSession.close();
        }
    }

    @Override
    public void updateMaxTimestamp(Integer workerId, Long maxTimestamp) {
        SqlSession sqlSession = sqlSessionFactory.openSession();
        try {
            LeafWorkerIdAlloc workerIdAlloc = new LeafWorkerIdAlloc();
            workerIdAlloc.setId(workerId);
            workerIdAlloc.setMaxTimestamp(maxTimestamp);
            sqlSession.update("com.sankuai.inf.leaf.segment.dao.WorkerIdAllocMapper.updateMaxTimestamp", workerIdAlloc);
            sqlSession.commit();
        } finally {
            sqlSession.close();
        }
    }

}
