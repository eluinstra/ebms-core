package nl.clockwork.ebms.transaction;

import javax.transaction.SystemException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.DependsOn;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.jta.JtaTransactionManager;

import com.atomikos.icatch.jta.UserTransactionManager;

import bitronix.tm.TransactionManagerServices;
import lombok.val;

public class TransactionManagerConfig
{
	@Value("${transactionManager.type}")
	TransactionManagerType transactionManagerType;

	public enum TransactionManagerType
	{
		DEFAULT,BITRONIX,ATOMIKOS;
	}

	@Bean("jtaTransactionManager")
	@DependsOn("btmConfig")
	public PlatformTransactionManager jtaTransactionManager() throws SystemException
	{
		switch (transactionManagerType)
		{
			case BITRONIX:
				val transactionManager = TransactionManagerServices.getTransactionManager();
				return new JtaTransactionManager(transactionManager,transactionManager);
			case ATOMIKOS:
				val userTransactionManager = new UserTransactionManager();
				userTransactionManager.setTransactionTimeout(300);
				userTransactionManager.setForceShutdown(true);
				return new JtaTransactionManager(userTransactionManager,userTransactionManager);
			default:
				return new NoOpTransactionManager();
		}
	}

	@Bean("btmConfig")
	public void btmConfig()
	{
		bitronix.tm.Configuration config = TransactionManagerServices.getConfiguration();
		config.setServerId("EbMSTransactionManager");
		config.setDefaultTransactionTimeout(300);
	}
}
