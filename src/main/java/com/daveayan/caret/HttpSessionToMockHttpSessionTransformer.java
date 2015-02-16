package com.daveayan.caret;

import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

import javax.servlet.http.HttpSession;

import org.springframework.mock.web.MockHttpSession;

import com.daveayan.mirage.ReflectionUtils;
import com.daveayan.transformers.CanTransform;
import com.daveayan.transformers.Context;


public class HttpSessionToMockHttpSessionTransformer implements CanTransform {

	public boolean canTransform(Object from, Class<?> to, String fieldName, Context context) {
		if(ReflectionUtils.classImplements(from.getClass(), HttpSession.class)) {
			if(ReflectionUtils.classIsOfType(to, MockHttpSession.class)) {
				return true;
			}
		}
		return false;
	}

	public Object transform(Object from, Class<?> to, String fieldName, Context context) {
		HttpSession inSession = (HttpSession) from;
		MockHttpSession outSession = new MockHttpSession();

		Set<String> exceptAttributeNames = (Set<String>) context.get("exceptAttributes");
		if(exceptAttributeNames == null || exceptAttributeNames.isEmpty()) {
			exceptAttributeNames = new HashSet<String>();
		}
		Set<String> attributeNames = (Set<String>) context.get("attributes");
		if(attributeNames == null || attributeNames.isEmpty()) {
			attributeNames = new HashSet<String>();
			Enumeration<String> attributes = inSession.getAttributeNames();
			int i = 0;
			while(attributes.hasMoreElements()) {
				String key = attributes.nextElement();
				attributeNames.add(key);
				i++;
			}
		}

		for(String name: attributeNames) {
			if(exceptAttributeNames.contains(name)) {
				continue;
			}
			Object value = inSession.getAttribute(name);
			if(value != null) {
				outSession.setAttribute(name, value);
			}
		}

		return outSession;
	}

}
