package com.daveayan.caret;

public class Caret1 {
	@SuppressWarnings("rawtypes")
	private Class classUnderTest;
	private String methodUnderTest;
	private long id = 0l;
	private boolean isRecording = false;

	public String getScript() {
		if(isRecording) {
			String test_name = classUnderTest.getSimpleName() + "_" + methodUnderTest + "_Test_" + id + "()  throws IOException {";
			String script = "\n@Test public void " + test_name;

			script += "\n}";
			return script;
		} else {
			return "Nothing recorded";
		}
	}

	public Caret1 startRecording() {
		isRecording = true;
		return this;
	}

	public Caret1 stopRecording() {
		isRecording = false;
		return this;
	}

	@SuppressWarnings("rawtypes")
	public static Caret1 testing(Class classUnderTest, String methodUnderTest) {
		Caret1 c = new Caret1();
		c.classUnderTest = classUnderTest;
		c.methodUnderTest = methodUnderTest;
		return c;
	}

	private Caret1() {}
}
