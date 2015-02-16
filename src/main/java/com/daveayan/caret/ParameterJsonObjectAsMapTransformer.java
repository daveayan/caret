package com.daveayan.caret;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

import com.daveayan.json.JSONArray;
import com.daveayan.json.JSONObject;
import com.daveayan.mirage.ReflectionUtils;
import com.daveayan.rjson.transformer.JsonToObjectTransformer;
import com.daveayan.transformers.Context;

public class ParameterJsonObjectAsMapTransformer implements JsonToObjectTransformer {
	public boolean canTransform(Object from, Class<?> to, String fieldName, Context context) {
		if(from == null) { return false; }
		return (from instanceof JSONObject)
				&& !((JSONObject) from).has("jvm_class_name")
				&& ReflectionUtils.classImplements(to, Map.class)
				&& StringUtils.equalsIgnoreCase(fieldName, "parameters");
	}

	public Object transform(Object from, Class<?> to, String fieldName, Context context) {
		System.out.println("\n\n\n\n\n----- HERE -----\n\n\n\n\n");
		JSONObject jo = (JSONObject) from;
		Map<Object, Object> newMap = null;
		if(StringUtils.equalsIgnoreCase(to.getName(), "java.util.Map")) {
			newMap = new HashMap<Object, Object>();
		} else {
			newMap = (Map<Object, Object>) ReflectionUtils.objectFor(to.getName());
		}
		Iterator<?> iter = jo.getMap().keySet().iterator();
		while (iter.hasNext()) {
			Object key = iter.next();
			JSONArray value = (JSONArray) jo.getMap().get(key);
			String[] strArray = value.getList().toArray(new String[] {});
			newMap.put(key, strArray);

//			Class convertTo = to;
//			if(StringUtils.equalsIgnoreCase(value.getClass().getName(), "com.daveayan.json.JSONArray")) {
//				convertTo = (new String[] {}).getClass();
//			}
//			newMap.put(key, context.transformer().delegateTransformation(value, convertTo, fieldName, context));
		}
		return newMap;
	}
}