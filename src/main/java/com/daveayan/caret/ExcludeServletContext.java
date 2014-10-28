package com.daveayan.caret;

import java.lang.reflect.Field;

import org.apache.commons.lang.StringUtils;

import com.daveayan.rjson.domain.Exclusion;
import com.daveayan.transformers.Context;

public class ExcludeServletContext implements Exclusion {

	public boolean exclude(Field field, Object from, Class<?> to,
			Context context) {
		if(StringUtils.equalsIgnoreCase(field.getName(), "servletContext")) {
			return true;
		}
		return false;
	}

}
