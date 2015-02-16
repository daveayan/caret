package com.daveayan.caret;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;

import com.daveayan.mirage.ReflectionUtils;
import com.daveayan.rjson.Rjson;
import com.daveayan.transformers.Context;
import com.daveayan.transformers.Transformer;

public class Caret {
	private Class classUnderTest;
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
	private Set<String> sessionAttributesToRecord = new HashSet<String>();
	private Set<String> sessionAttributesToNotRecord = new HashSet<String>();
	private Set<String> requestAttributesToRecord = new HashSet<String>();
	private Set<String> requestAttributesToNotRecord = new HashSet<String>();

	private int executionId = 0;

	private boolean startRecording = false;

	public void startRecording() {
		startRecording = true;
	}

	public void pauseRecording() {
		startRecording = false;
	}

	public Caret sessionAttributesToRecord(String... attributes) {
		sessionAttributesToRecord.addAll(Arrays.asList(attributes));
		return this;
	}

	public Caret sessionAttributesToNotRecord(String... attributes) {
		sessionAttributesToNotRecord.addAll(Arrays.asList(attributes));
		return this;
	}

	public Caret requestAttributesToRecord(String... attributes) {
		requestAttributesToRecord.addAll(Arrays.asList(attributes));
		return this;
	}

	public Caret requestAttributesToNotRecord(String... attributes) {
		requestAttributesToNotRecord.addAll(Arrays.asList(attributes));
		return this;
	}

	public String getTestScript() {
		String test_name = classUnderTest.getSimpleName() + "_" + methodUnderTest + "_Test_" + id + "()  throws IOException {";
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
		script += "\n\t" + classUnderTest.getSimpleName() + " objectUnderTest = new " + classUnderTest.getSimpleName() + "();";

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

	public void captureRequestAndSession(HttpServletRequest request, HttpSession session) {
		if(request != null && session == null) {
			Transformer t = Transformer.newInstance().with_a(new HttpServletRequestToMockTransformer());
			requestAttributesToRecord = new HashSet<String>();
			requestAttributesToRecord.add("___NO_SUCH_REQUEST_ATTRIBUTE___");
			Context context = Context.newInstance().put("attributes", requestAttributesToRecord).and("exceptAttributes", requestAttributesToNotRecord);
			MockHttpServletRequest newRequest = (MockHttpServletRequest) t.transform(request, MockHttpServletRequest.class, "", context);
			MockHttpSession newSession = new MockHttpSession();
			newRequest.setSession(newSession);
			captureInputParameter(newRequest, null, "");
			return;
		} else if(request == null && session != null) {
			Transformer t = Transformer.newInstance().with_a(new HttpSessionToMockHttpSessionTransformer());
			Context context = Context.newInstance().put("attributes", sessionAttributesToRecord).and("exceptAttributes", sessionAttributesToNotRecord);
			MockHttpSession newSession = (MockHttpSession) t.transform(session, MockHttpSession.class, "", context);
			MockHttpServletRequest newRequest = new MockHttpServletRequest();
			newRequest.setSession(newSession);
			captureInputParameter(newRequest, null, "");
		}
	}

	public void captureHttpSession(HttpSession session) {
		Transformer t = Transformer.newInstance().with_a(new HttpSessionToMockHttpSessionTransformer());
		Context context = Context.newInstance().put("attributes", sessionAttributesToRecord).and("exceptAttributes", sessionAttributesToNotRecord);
		MockHttpSession newSession = (MockHttpSession) t.transform(session, MockHttpSession.class, "", context);
		captureInputParameter(newSession, null, "");
	}

	public void captureHttpServletRequest(HttpServletRequest request) {
		Transformer t = Transformer.newInstance().with_a(new HttpServletRequestToMockTransformer());
		Context context = Context.newInstance().put("attributes", requestAttributesToRecord).and("exceptAttributes", requestAttributesToNotRecord);
		MockHttpServletRequest newRequest = (MockHttpServletRequest) t.transform(request, MockHttpServletRequest.class, "", context);
		captureInputParameter(newRequest, null, "");
	}

	public void captureHttpServletRequestNoAttributes(HttpServletRequest request) {
		Transformer t = Transformer.newInstance().with_a(new HttpServletRequestToMockTransformer());
		requestAttributesToRecord = new HashSet<String>();
		requestAttributesToRecord.add("___NO_SUCH_REQUEST_ATTRIBUTE___");
		Context context = Context.newInstance().put("attributes", requestAttributesToRecord).and("exceptAttributes", requestAttributesToNotRecord);
		MockHttpServletRequest newRequest = (MockHttpServletRequest) t.transform(request, MockHttpServletRequest.class, "", context);
		captureInputParameter(newRequest, null, "");
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
		if(! startRecording) {
			Object returnValue = ReflectionUtils.call_forcibly(targetObject, methodName, parameters);
			return returnValue;
		}
		String mockName = captureAsMock(targetObject);
		String inputParamStringForMock = "";
		for(int i = 0; i < parameters.length ; i++) {
			executionId ++;
			Object transformedObject = parameters[i];
			Object originalObject = null;
			if(transformedObject instanceof HttpSession) {
				originalObject = transformedObject;
				Transformer t = Transformer.newInstance().with_a(new HttpSessionToMockHttpSessionTransformer());
				Context context = Context.newInstance().put("attributes", sessionAttributesToRecord).and("exceptAttributes", sessionAttributesToNotRecord);
				transformedObject = (MockHttpSession) t.transform((HttpSession) transformedObject, MockHttpSession.class, "", context);
			}
			if(transformedObject instanceof HttpServletRequest) {
				originalObject = transformedObject;
				Transformer t = Transformer.newInstance().with_a(new HttpServletRequestToMockTransformer());
				Context context = Context.newInstance().put("attributes", requestAttributesToRecord).and("exceptAttributes", requestAttributesToNotRecord);
				transformedObject = (MockHttpServletRequest) t.transform((HttpServletRequest) transformedObject, MockHttpServletRequest.class, "", context);
			}
			String mockVariableName = captureDomainObject(originalObject, transformedObject, "mock");
			if(i != 0) inputParamStringForMock += ",";
			inputParamStringForMock += mockVariableName;
		}
		executionId ++;
		Object returnValue = ReflectionUtils.call_forcibly(targetObject, methodName, parameters);
		if(returnValue != null) {
			String mockVariableName = captureDomainObject(null, returnValue, "mock");
			String mock = "when(" + mockName + "." + methodName + "(" + inputParamStringForMock + ")).thenReturn(" + mockVariableName + ");";
			linesOfCode.add("\n\t" + mock + "\n");
		} else {
			String mock = "when(" + mockName + "." + methodName + "(" + inputParamStringForMock + "));";
			linesOfCode.add("\n\t" + mock + "\n");
		}


		return returnValue;
	}

	public String captureAsMock(Object objectToCapture) {
		String simpleName = objectToCapture.getClass().getSimpleName();
		String mockName = "mock" + simpleName;
		String mockString = simpleName + " " + mockName + " = mock(" + simpleName + ".class);";
		mocks.add("\n\t" + mockString);
		return mockName;
	}

	public void captureInputParameter(Object objectToCapture, Class typeOfParamIfNull, String parameterNameIfNull) {
		executionId ++;

		if(objectToCapture == null) {
			String simpleName = typeOfParamIfNull.getSimpleName();
			String mockVariableName = "input" + parameterNameIfNull + "_" + executionId;
			String line = simpleName + " " + mockVariableName + " = null;";
			linesOfCode.add("\n\t" + line);
			methodCallParams.add(mockVariableName);
			return;
		}

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

		String inputVariableName = captureDomainObject(null, objectToCapture, "input");

		methodCallParams.add(inputVariableName);
	}

	private String captureDomainObjectIgnoringRecorded(Object originalObject, Object transformedObject, String prefix) {
		if(originalObject == null) {
			String identity = getIdentity(transformedObject);
			String locationToJsonCaptured = captureObject(transformedObject, Integer.toString(executionId));
			String simpleName = transformedObject.getClass().getSimpleName();
			String mockVariableName = prefix + simpleName + "_" + executionId;
			String line = simpleName + " " + mockVariableName + " = (" + simpleName + ")  RjsonUtil.fileAsObject(\"" + locationToJsonCaptured + "\");";
			linesOfCode.add("\n\t" + line);
			recordedObjects.put(identity, mockVariableName);
			return mockVariableName;
		} else {
			String identity = getIdentity(originalObject);
			String locationToJsonCaptured = captureObject(transformedObject, Integer.toString(executionId));
			String simpleName = transformedObject.getClass().getSimpleName();
			String mockVariableName = prefix + simpleName + "_" + executionId;
			String line = simpleName + " " + mockVariableName + " = (" + simpleName + ")  RjsonUtil.fileAsObject(\"" + locationToJsonCaptured + "\");";
			linesOfCode.add("\n\t" + line);
			recordedObjects.put(identity, mockVariableName);
			return mockVariableName;
		}
	}

	private String captureDomainObject(Object originalObject, Object transformedObject, String prefix) {
		if(originalObject == null) {
			String identity = getIdentity(transformedObject);
			String recordedVariableName = recordedObjects.get(identity);
			if(StringUtils.isNotBlank(recordedVariableName)) {
				return recordedVariableName;
			}
			return captureDomainObjectIgnoringRecorded(originalObject, transformedObject, prefix);
		} else {
			String identity = getIdentity(originalObject);
			String recordedVariableName = recordedObjects.get(identity);
			if(StringUtils.isNotBlank(recordedVariableName)) {
				return recordedVariableName;
			}
			return captureDomainObjectIgnoringRecorded(originalObject, transformedObject, prefix);
		}
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

	public Rjson getRjsonInstance() {
		Rjson rjson = Rjson.newInstance().andRecordAllModifiers().andRecordFinal().with(new ExcludeServletContext()).with(new CachedDateTimeZoneTransformer());
		return rjson;
	}

	public static Object fileAsObject(String fileName) throws IOException {
		return Rjson.newInstance().with(new ParameterJsonObjectAsMapTransformer()).toObject(fileAsString(fileName));
	}

	public static String fileAsString(String file) throws IOException {
		return FileUtils.readFileToString(new File(file)).replaceAll("\\r\\n", "\n");
	}

	public static Caret testing(Class classUnderTest, String methodUnderTest, String outputFolder) {
		Caret a = Caret.testing(classUnderTest, methodUnderTest, outputFolder, System.currentTimeMillis());
		return a;
	}

	public static Caret testingPause(Class classUnderTest, String methodUnderTest, String outputFolder) {
		Caret a = Caret.testing(classUnderTest, methodUnderTest, outputFolder, System.currentTimeMillis());
		a.startRecording = false;
		return a;
	}

	public static Caret testing(Class classUnderTest, String methodUnderTest, String outputFolder, long id) {
		Caret a = new Caret();
		a.classUnderTest = classUnderTest;
		a.methodUnderTest = methodUnderTest;
		a.outputFolder = outputFolder;
		a.id = id;
		a.folderInstanceId = Long.toString(id);
		a.startRecording = true;
		return a;
	}

	private Caret() {}
}
