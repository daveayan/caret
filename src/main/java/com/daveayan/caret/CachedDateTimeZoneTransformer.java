package com.daveayan.caret;

import org.joda.time.tz.CachedDateTimeZone;

import com.daveayan.rjson.transformer.BaseTransformer;
import com.daveayan.transformers.CanTransform;
import com.daveayan.transformers.Context;

public class CachedDateTimeZoneTransformer extends BaseTransformer implements CanTransform {

	public String transform(Object from, Class< ? > to, String fieldName, Context context) {
		if(cycleDetectedWith(from, context)) return null;
		printData(null, context);
		return null;
	}

	public boolean canTransform(Object from, Class< ? > to, String fieldName, Context context) {
		return from != null && from instanceof CachedDateTimeZone;
	}

}
