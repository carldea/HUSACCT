package husacct.control;

import husacct.ServiceProvider;
import husacct.common.dto.ApplicationDTO;
import husacct.common.savechain.ISaveable;
import husacct.common.services.IConfigurable;
import husacct.common.services.ObservableService;
import husacct.control.domain.Workspace;
import husacct.control.presentation.util.DialogUtils;
import husacct.control.presentation.util.GeneralConfigurationPanel;
import husacct.control.task.ApplicationController;
import husacct.control.task.BootstrapHandler;
import husacct.control.task.CodeViewController;
import husacct.control.task.MainController;
import husacct.control.task.StateController;
import husacct.control.task.States;
import husacct.control.task.ViewController;
import husacct.control.task.WorkspaceController;
import husacct.control.task.configuration.ConfigurationManager;
import husacct.control.task.configuration.NonExistingSettingException;
import husacct.control.task.threading.ThreadWithLoader;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.swing.JDialog;
import javax.swing.JPanel;

import org.apache.log4j.Logger;
import org.jdom2.Element;


public class ControlServiceImpl extends ObservableService implements IControlService, ISaveable, IConfigurable {

	private Logger logger = Logger.getLogger(ControlServiceImpl.class);
	ArrayList<ILocaleChangeListener> listeners = new ArrayList<ILocaleChangeListener>();
	
	private MainController mainController; 
	private WorkspaceController workspaceController;
	private ApplicationController applicationController;
	private StateController stateController;
	private ViewController viewController;
	private ConfigurationManager configurationManager;
	private CodeViewController codeViewController;
	private GeneralConfigurationPanel generalConfigurationPanel;
	
	public ControlServiceImpl(){
		logger.debug("Starting HUSACCT");
		mainController = new MainController();
		workspaceController = mainController.getWorkspaceController();
		applicationController = mainController.getApplicationController();
		stateController = mainController.getStateController();
		viewController = mainController.getViewController();
		configurationManager = new ConfigurationManager();
		mainController.initialiseCodeViewerController();
		codeViewController = mainController.getCodeViewerController();
	}
	
	@Override
	public void parseCommandLineArguments(String[] commandLineArguments){
		mainController.parseCommandLineArguments(commandLineArguments);
	}
	
	@Override
	public void startApplication() {
		mainController.startGui();

		if(mainController.getCommandLineController().getResult().contains("bootstrap")){
			new BootstrapHandler(mainController.getCommandLineController().getResult().getStringArray("bootstrap"));
		}
	}
	
	@Override
	public Element getWorkspaceData() {
		Element data = new Element("workspace");
		Workspace workspace = workspaceController.getCurrentWorkspace();
		data.setAttribute("name", workspace.getName());
		data.setAttribute("language", ServiceProvider.getInstance().getLocaleService().getLocale().getLanguage());

		return data;
	}
	
	@Override
	public void loadWorkspaceData(Element workspaceData) {
		try {
			String workspaceName = workspaceData.getAttributeValue("name");
			String languageName = workspaceData.getAttributeValue("language");
			workspaceController.createWorkspace(workspaceName);
			ServiceProvider.getInstance().getLocaleService().setLocale(new Locale(languageName));
		} catch (Exception e){
			logger.debug("WorkspaceData corrupt: " + e);
		}
	}
	
	@Override
	public void showErrorMessage(String message){
		applicationController.showErrorMessage(message);
	}
	
	@Override
	public void showInfoMessage(String message){
		applicationController.showInfoMessage(message);
	}
	
	@Override
	public void centerDialog(JDialog dialog){
		DialogUtils.alignCenter(dialog);
	}
	
	@Override
	public ThreadWithLoader getThreadWithLoader(String progressInfoText, Runnable threadTask) {
		ThreadWithLoader loader = new ThreadWithLoader(mainController, progressInfoText, threadTask);
		
		mainController.getApplicationController().setCurrentLoader(loader.getLoader());
		
		return loader;
	}
	
	@Override
	public void setServiceListeners(){
		stateController.setServiceListeners();
		viewController.setLocaleListeners();
	}
	
	public MainController getMainController(){
		return mainController;
	}
	
	@Override
	public void finishPreAnalysing() {
		mainController.getStateController().addState(States.PREANALYSED);
	}
	
	@Override
	public boolean isPreAnalysed() {
		return getState().contains(States.PREANALYSED);
	}
	
	@Override
	public List<States> getState() {
		return this.getMainController().getStateController().getState();
	}
	

	@Override
	public void updateProgress(int progressPercentage) {
		mainController.getApplicationController().getCurrentLoader().setProgressText(progressPercentage);
	}
	
	@Override
	public void setValidate(boolean validate) {
		this.mainController.getStateController().setValidating(validate);
		this.mainController.getStateController().checkState();
	}

	@Override
	public ApplicationDTO getApplicationDTO() {
		return mainController.getWorkspaceController().getCurrentWorkspace().getApplicationData();
	}

	@Override
	public String getProperty(String key) throws NonExistingSettingException {
		return configurationManager.getProperty(key);
	}

	@Override
	public int getPropertyAsInteger(String key) throws NonExistingSettingException, NumberFormatException {
		return configurationManager.getPropertyAsInteger(key);
	}

	@Override
	public boolean getPropertyAsBoolean(String key) throws NonExistingSettingException {
		return configurationManager.getPropertyAsBoolean(key);
	}
	
	@Override
	public void setProperty(String key, String value) {
		configurationManager.setProperty(key, value);
	}

	@Override
	public void setPropertyFromInteger(String key, int value) {
		configurationManager.setPropertyFromInteger(key, value);
	}

	@Override
	public void setPropertyFromBoolean(String key, boolean value) {
		configurationManager.setPropertyFromBoolean(key, value);
	}
	
	@Override
	public void displayErrorsInFile(String fileName, ArrayList<Integer> errors) {
		codeViewController.displayErrorsInFile(fileName, errors);
	}

	@Override
	public String getConfigurationName() {
		return ServiceProvider.getInstance().getLocaleService().getTranslatedString("ConfigGeneral");
	}

	@Override
	public JPanel getConfigurationPanel() {
		if (generalConfigurationPanel == null)
			generalConfigurationPanel = new GeneralConfigurationPanel();
		return generalConfigurationPanel;
	}

	@Override
	public void saveConfig() {
		configurationManager.storeProperties();
	}
}
