package net.jotorren.microservices.tx;

import java.util.List;

import net.jotorren.microservices.context.ThreadLocalContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

public abstract class CompositeTransactionParticipantService {

	private static final Logger LOG = LoggerFactory.getLogger(CompositeTransactionParticipantService.class);

	public static final String CURRENT_TRANSACTION_KEY = "current.opened.tx";
	
	@Autowired
	private CompositeTransactionManager txManager;
	
	public CompositeTransactionManager getCompositeTransactionManager(){
		return txManager;
	}

	public void cancel(String txId) {
		ThreadLocalContext.remove(CURRENT_TRANSACTION_KEY);
		
		LOG.warn("Rolling back transaction [{}]", txId);
	}
	
	@Transactional(readOnly=false)
	public void confirm(String txId) {
		ThreadLocalContext.remove(CURRENT_TRANSACTION_KEY);
		
		LOG.warn("Looking for transaction [{}]", txId);
		List<EntityCommand<?>> transactionOperations = txManager.fetch(txId);
		if (null == transactionOperations){
			LOG.warn("Transaction [{}] does not exist. Ignoring commit call", txId);
			return;
		}
		CompositeTransactionDao unsynchronizedDao = getCompositeTransactionDao();
		unsynchronizedDao.apply(transactionOperations);
		
		LOG.warn("Committing transaction [{}]", txId);
		unsynchronizedDao.commit();
	}
		
	public abstract CompositeTransactionDao getCompositeTransactionDao();
}