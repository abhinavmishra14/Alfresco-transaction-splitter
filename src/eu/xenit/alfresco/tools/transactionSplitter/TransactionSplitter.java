package eu.xenit.alfresco.tools.transactionSplitter;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.action.ParameterDefinitionImpl;
import org.alfresco.repo.action.executer.ActionExecuterAbstractBase;
import org.alfresco.repo.policy.BehaviourFilter;
import org.alfresco.repo.transaction.RetryingTransactionHelper.RetryingTransactionCallback;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.action.ParameterDefinition;
import org.alfresco.service.cmr.dictionary.DataTypeDefinition;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.transaction.TransactionService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * 
 * @author iBlanco and Thijs Lemmens
 *
 */

public class TransactionSplitter extends ActionExecuterAbstractBase {

	public static final Log LOG = LogFactory.getLog(TransactionSplitter.class);
	public static final String NAME = "TransactionSplitter";
	public static final String PARAM_NMBOFNODES = "NmbOfNodes";
	public static final String PARAM_NODEREFS = "NodeRefs";

	private ServiceRegistry serviceRegistry;
	private BehaviourFilter behaviourFilter;
	private boolean removeAspect = true;
	private int totalBatchNodes = 50;

	public void setServiceRegistry(ServiceRegistry serviceRegistry) {
		this.serviceRegistry = serviceRegistry;
	}

	public void setBehaviourFilter(BehaviourFilter behaviourFilter) {
		this.behaviourFilter = behaviourFilter;
	}

	public void setRemoveAspect(boolean removeAspect) {
		this.removeAspect = removeAspect;
	}
	
	public void setTotalBatchNodes(int totalBatchNodes){
		this.totalBatchNodes = totalBatchNodes;
	}

	@Override
	protected void executeImpl(Action action, NodeRef actionedUponNodeRef) {
		List<NodeRef> nodeRefs = getNodeRefs(action, actionedUponNodeRef);
				
		//Override the default value of totalBatchNodes
		if(action.getParameterValue(PARAM_NMBOFNODES)!=null){
				totalBatchNodes = (Integer) action
					.getParameterValue(PARAM_NMBOFNODES);
		}

		List<NodeRef> batch = new ArrayList<NodeRef>();

		NodeRef node = null;
		int total = nodeRefs.size();
		int totalCount = 0;
		int lastPercentage = 0;
		int count = 0;
		
		LOG.info("Starting to process "+total+" nodes in transactions of "+totalBatchNodes+" nodes.");

		Iterator<NodeRef> iter = nodeRefs.iterator();
		while (iter.hasNext()) {

			if (count == totalBatchNodes) {
				totalCount += count;
				addAspect(batch);
				int percentage = (totalCount * 100)/total;
				if(percentage >= lastPercentage+10 ){
					lastPercentage += 10;
					LOG.info("TransactionSplitter reached "+lastPercentage+"%");
				}
				batch = new ArrayList<NodeRef>();
				count = 0;
			}

			node = iter.next();

		
			batch.add(node);
			count++;

		}
		
		if(batch.size()>0){
			addAspect(batch);
		}
		LOG.info("TransactionSplitter reached 100%");
		LOG.info("Completed TransactionSplitter!");

	}

	@SuppressWarnings("unchecked")
	private List<NodeRef> getNodeRefs(Action action, NodeRef actionedUponNodeRef) {
		List<NodeRef> nodeRefs = null;
		NodeService nodeService = serviceRegistry.getNodeService();
		
		//NodeRefs can be given as parameters
		if(action.getParameterValue(PARAM_NODEREFS)!=null){
			nodeRefs = (List<NodeRef>) action.getParameterValue(PARAM_NODEREFS);
		}
		//Or read from the node on which this action is executed
		else{
			LOG.info("Reading nodes from repository file.");
			if (nodeService.exists(actionedUponNodeRef)){
				nodeRefs = new ArrayList<NodeRef>();
				
				// Read the content from the respository
				ContentReader reader = serviceRegistry.getContentService().getReader(actionedUponNodeRef, ContentModel.PROP_CONTENT);
				InputStream in = reader.getContentInputStream();
				BufferedReader br = new BufferedReader(
						new InputStreamReader(in));
				String strLine;
				// Read File Line By Line
				try {
					while ((strLine = br.readLine()) != null) {
						NodeRef node = new NodeRef(strLine);
						if(nodeService.exists(node)){
							nodeRefs.add(node);
							LOG.debug("Node "+strLine+" added to list of noderefs");
						}
						else{
							LOG.warn("Node "+strLine+" does not exist");
						}
					}
					// Close the input stream
					in.close();

				} catch (IOException e) {
					LOG.error("Error reading content",e);
				}

			}
		}
		return nodeRefs;
	}



	private void addAspect(final List<NodeRef> list) {
		RetryingTransactionCallback<Void> txnFile = new RetryingTransactionCallback<Void>() {

			@Override
			public Void execute() throws Throwable {
				behaviourFilter.disableBehaviour(ContentModel.ASPECT_AUDITABLE);
				NodeService nodeService = serviceRegistry.getNodeService();
				QName dummyAspect = QName.createQName("dummy.model",
						"dummyAspect");

				Iterator<NodeRef> iter = list.iterator();
				NodeRef nodeRef = null;

				LOG.debug("Processing a list of " + list.size() + " nodes");
				while (iter.hasNext()) {
					nodeRef = iter.next();
					nodeService.addAspect(nodeRef, dummyAspect, null);
					if (removeAspect)
						nodeService.removeAspect(nodeRef, dummyAspect);
				}

				return null;
			}
		};
		TransactionService transactionService = serviceRegistry
				.getTransactionService();
		transactionService.getRetryingTransactionHelper().doInTransaction(
				txnFile, false, true);

	}
	
	@Override
	protected String getTitleKey() {
		return NAME;
	};
	
	@Override
	protected String getDescriptionKey(){
		return NAME;
	}

	@Override
	protected void addParameterDefinitions(List<ParameterDefinition> paramList) {
		paramList
		.add(new ParameterDefinitionImpl(PARAM_NODEREFS,
				DataTypeDefinition.ANY, false,
				"Number of nodes per transaction"));
		paramList
				.add(new ParameterDefinitionImpl(PARAM_NMBOFNODES,
						DataTypeDefinition.INT, false,
						"Number of nodes per transaction"));

	}

}
