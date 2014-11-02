package com.daveayan.caret;

import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.springframework.mock.web.MockHttpServletRequest;

import com.daveayan.mirage.ReflectionUtils;
import com.daveayan.transformers.CanTransform;
import com.daveayan.transformers.Context;

public class HttpServletRequestToMockTransformer implements CanTransform {

	public boolean canTransform(Object from, Class<?> to, Context context) {
		if(ReflectionUtils.classImplements(from.getClass(), HttpServletRequest.class)) {
			if(ReflectionUtils.classIsOfType(to, MockHttpServletRequest.class)) {
				return true;
			}
		}
		return false;
	}

	public Object transform(Object from, Class<?> to, Context context) {
		MockHttpServletRequest outRequest = new MockHttpServletRequest();
		HttpServletRequest inRequest = (HttpServletRequest) from;

		Set<String> exceptAttributeNames = (Set<String>) context.get("exceptAttributes");
		if(exceptAttributeNames == null || exceptAttributeNames.isEmpty()) {
			exceptAttributeNames = new HashSet<String>();
		}
		Set<String> attributeNames = (Set<String>) context.get("attributes");
		if(attributeNames == null || attributeNames.isEmpty()) {
			attributeNames = new HashSet<String>();
			Enumeration<String> attributes = inRequest.getAttributeNames();
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
			Object value = inRequest.getAttribute(name);
			if(value != null) {
				outRequest.setAttribute(name, value);
			}
		}

		Enumeration params = inRequest.getParameterNames();
		while(params.hasMoreElements()) {
			String key = (String) params.nextElement();
			String value = inRequest.getParameter(key);
			if(value != null) {
				outRequest.setParameter(key, value);
			}
		}

		return outRequest;
	}

}
