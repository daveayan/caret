package com.daveayan.caret;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;

import com.daveayan.mirage.ReflectionUtils;
import com.daveayan.rjson.Rjson;
import com.daveayan.rjson.utils.RjsonUtil;

public class Caret {
	private Object objectUnderTest;
	private String methodUnderTest, outputFolder, folderInstanceId;
	private long id;
	private Set<String> mocks = new HashSet<String>();
	private List<String> linesOfCode = new ArrayList<String>();
	private List<String> inputParams = new ArrayList<String>();
	private Map<String, String> recordedObjects = new HashMap<String, String>();
	private List<String> methodCallParams = new ArrayList<String>();
	private String returnLine = "";
	private String returnConversion = "";
	private String assertion = "";

	private int executionId = 0;

	public String getTestScript() {
		String test_name = objectUnderTest.getClass().getSimpleName() + "_" + methodUnderTest + "_Test_" + id + "()  throws IOException {";
		String script = "\n@Test public void " + test_name;

		for(String mock: mocks) {
			script += mock;
		}

		script += "\n";

		for(String lc: linesOfCode) {
			script += lc;
		}

		for(String lc: inputParams) {
			script += lc;
		}
		script += "\n\t" + objectUnderTest.getClass().getSimpleName() + " objectUnderTest = new " + objectUnderTest.getClass().getSimpleName() + "();";

		script += "\n\t// Set the dependencies here ...";

		script += "\n\t" + returnLine + "objectUnderTest." + methodUnderTest + "(";
		for(int i = 0; i < methodCallParams.size(); i ++) {
			String s = methodCallParams.get(i);
			if(i != 0) {script += ", "; }
			script += s;
		}
		script += ");";

		script += "\n\t" + returnConversion;
		script += "\n\t" + assertion;

		script += "\n}";

		File scriptFile = new File(outputFolder + "/" + folderInstanceId + "/script.java");
		try {
			System.out.println("Wrote file " + scriptFile.getAbsolutePath());
			FileUtils.write(scriptFile, script);
		} catch (IOException e) {
			System.out.println("Cannot write file " + scriptFile.getAbsolutePath());
			e.printStackTrace();
		}

		return script;
	}

	public void captureExpectedReturnObject(Object object) {
		String locationToJsonCaptured = captureObject(object, Integer.toString(executionId));
		String simpleName = object.getClass().getSimpleName();
		String mockVariableName = "expectedReturnJson" + simpleName + "_" + executionId;

		String line = "String " + mockVariableName + " = FileUtils.readFileToString(new File(\"" + locationToJsonCaptured + "\"));";
		linesOfCode.add("\n\t" + line);

		returnLine = simpleName + " actualResult = ";
		returnConversion = "String actualResultAsJson = RjsonUtil.completeSerializer().toJson(actualResult);";

		assertion = "Assert.assertEquals(" + mockVariableName + ", actualResultAsJson);";
	}

	public Object executeAndCaptureForMock(Object targetObject, String methodName, Object... parameters) {
		String mockName = captureAsMock(targetObject);
		String inputParamStringForMock = "";
		for(int i = 0; i < parameters.length ; i++) {
			executionId ++;
			Object p = parameters[i];
			String mockVariableName = captureDomainObject(p, "mock");
			if(i != 0) inputParamStringForMock += ",";
			inputParamStringForMock += mockVariableName;
		}
		executionId ++;
		Object returnValue = ReflectionUtils.call_forcibly(targetObject, methodName, parameters);
		String mockVariableName = captureDomainObject(returnValue, "mock");

		String mock = "when(" + mockName + "." + methodName + "(" + inputParamStringForMock + ")).thenReturn(" + mockVariableName + ");";
		linesOfCode.add("\n\t" + mock + "\n");

		return returnValue;
	}

	public String captureAsMock(Object objectToCapture) {
		String simpleName = objectToCapture.getClass().getSimpleName();
		String mockName = "mock" + simpleName;
		String mockString = simpleName + " " + mockName + " = mock(" + simpleName + ".class);";
		mocks.add("\n\t" + mockString);
		return mockName;
	}

	public String captureInputParameter(Object objectToCapture) {
		executionId ++;
		Rjson rjson = getRjsonInstance();
		String json = rjson.toJson(objectToCapture);

		File fileToCreate = new File(outputFolder + "/" + folderInstanceId + "/" + objectToCapture.getClass().getName() + "_" + executionId + ".json");
		try {
			System.out.println("Writing to " + fileToCreate.getAbsolutePath());
			FileUtils.writeStringToFile(fileToCreate, json);
		} catch (IOException e) {
			System.out.println("Cannot capture " + fileToCreate.getAbsolutePath());
			e.printStackTrace();
		}

		String inputVariableName = captureDomainObject(objectToCapture, "input");

		methodCallParams.add(inputVariableName);

		return fileToCreate.getAbsolutePath();
	}

	private String captureDomainObjectIgnoringRecorded(Object object, String prefix) {
		String identity = getIdentity(object);
		String locationToJsonCaptured = captureObject(object, Integer.toString(executionId));
		String simpleName = object.getClass().getSimpleName();
		String mockVariableName = prefix + simpleName + "_" + executionId;
		String line = simpleName + " " + mockVariableName + " = (" + simpleName + ")  RjsonUtil.fileAsObject(\"" + locationToJsonCaptured + "\");";
		linesOfCode.add("\n\t" + line);
		recordedObjects.put(identity, mockVariableName);
		return mockVariableName;
	}

	private String captureDomainObject(Object object, String prefix) {
		String identity = getIdentity(object);
		String recordedVariableName = recordedObjects.get(identity);
		if(StringUtils.isNotBlank(recordedVariableName)) {
			return recordedVariableName;
		}
		return captureDomainObjectIgnoringRecorded(object, prefix);
	}

	private String captureObject(Object objectToCapture, String id) {
		Rjson rjson = getRjsonInstance();
		String json = rjson.toJson(objectToCapture);

		File fileToCreate = new File(outputFolder + "/" + folderInstanceId + "/" + objectToCapture.getClass().getName() + "_" + id + ".json");
		try {
			System.out.println("Writing to " + fileToCreate.getAbsolutePath());
			FileUtils.writeStringToFile(fileToCreate, json);
		} catch (IOException e) {
			System.out.println("Cannot capture " + fileToCreate.getAbsolutePath());
			e.printStackTrace();
		}

		return fileToCreate.getAbsolutePath();
	}

	private String getIdentity(Object object) {
		int id = System.identityHashCode(object);
		String identity = object.getClass().getName() + ":" + id;
		return identity;
	}

	private Rjson getRjsonInstance() {
		Rjson rjson = RjsonUtil.completeSerializer().andRecordFinal().with(new ExcludeServletContext());
		return rjson;
	}

	public static Caret testing(Object objectUnderTest, String methodUnderTest, String outputFolder) {
		Caret a = Caret.testing(objectUnderTest, methodUnderTest, outputFolder, System.currentTimeMillis());
		return a;
	}

	public static Caret testing(Object objectUnderTest, String methodUnderTest, String outputFolder, long id) {
		Caret a = new Caret();
		a.objectUnderTest = objectUnderTest;
		a.methodUnderTest = methodUnderTest;
		a.outputFolder = outputFolder;
		a.id = id;
		a.folderInstanceId = Long.toString(id);
		return a;
	}

	private Caret() {}
}
