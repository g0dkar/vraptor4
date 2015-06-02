package br.com.caelum.vraptor.http.iogi;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

import javax.enterprise.inject.Vetoed;

import net.vidageek.mirror.dsl.Mirror;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.com.caelum.iogi.Instantiator;
import br.com.caelum.iogi.parameters.Parameter;
import br.com.caelum.iogi.parameters.Parameters;
import br.com.caelum.iogi.reflection.Target;

/**
 * Implements a free form model instantiation based on {@link HashMap maps} and json-like structures
 * 
 * @author Rafael M. Lins (g0dkar)
 *
 */
@Vetoed
public class MapInstantiator implements Instantiator<Map<String, Object>> {
	private final Logger log = LoggerFactory.getLogger(MapInstantiator.class);
	private final Instantiator<Object> mapElementInstantiator;
	
	public MapInstantiator(final Instantiator<Object> mapElementInstantiator) {
		this.mapElementInstantiator = mapElementInstantiator;
	}
	
	/**
	 * @return {@code true} if we'll handle the creation of this parameter (in our case: {@link Map Maps})
	 */
	@Override
	public boolean isAbleToInstantiate(final Target<?> target) {
		if (log.isDebugEnabled()) { log.debug("Will type {} be handled by this class? {}", target.getClassType(), target.getClassType().isAssignableFrom(Map.class)); }
		return target.getClassType().isAssignableFrom(Map.class) && target.typeArgument(0).getClassType().equals(String.class);
	}
	
	/**
	 * Builds a {@link Map} from the specified parameters (dot path notation)
	 */
	@Override
	public Map<String, Object> instantiate(final Target<?> target, final Parameters parameters) {
		final Class<?> rootMapClass = target.getClassType();
		final Map<String, Object> map = mapForClass(rootMapClass);
		final List<Parameter> relevantParameters = parameters.forTarget(target);
		
		for (final Parameter parameter : relevantParameters) {
			put(rootMapClass, map, parameter.getName().substring(parameter.getName().indexOf(".") + 1), valueForParameter(target, parameter));
		}
		
		return map;
	}
	
	/**
	 * Returns an Object for the specified {@link Parameter}. It tries to stick to the Hungarian Notation
	 * ({@code iSomething} is an {@link Integer}, {@code dAnother} is a {@link Double} and so forth).
	 * 
	 * @param target The target
	 * @param parameter The parameter
	 * @return The converted value (or the value as a {@link String})
	 */
	private Object valueForParameter(final Target<?> target, final Parameter parameter) {
		final String name = parameter.getName(), targetName = name.substring(name.lastIndexOf(".") + 1);
		
		if (targetName.length() > 1 && targetName.substring(0, 1).matches("[ildfrxbtc]") && targetName.substring(1, 2).matches("[A-Z]")) {
			final char first = targetName.charAt(0);
			
			Class<?> targetClass = null;
			
			switch (first) {
				case 'i': targetClass = Integer.class; break;
				case 'l': targetClass = Long.class; break;
				case 'd': targetClass = Double.class; break;
				case 'f': targetClass = Float.class; break;
				case 'r': targetClass = BigDecimal.class; break;
				case 'x': targetClass = BigInteger.class; break;
				case 'b': targetClass = Boolean.class; break;
				case 't': targetClass = Date.class; break;
				case 'c': targetClass = Calendar.class; break;
			}
			
			// This damn thing doesn't do nothing. Always silently returns null. "Silently" as in "i'm too tired right now to set up logging"
			return mapElementInstantiator.instantiate(Target.create(targetClass, target.getName()), new Parameters(parameter));
		}
		
		return parameter.getValue();
	}
	
	/**
	 * Create a {@link Map} based on it's class. Defaults to {@link HashMap}.
	 * @param mapClass The {@link Map} class
	 * @return The {@link Map} instance
	 * @see LinkedHashMap
	 * @see TreeMap
	 */
	private Map<String, Object> mapForClass(final Class<?> mapClass) {
		if (Map.class.isAssignableFrom(mapClass)) {
			if (mapClass.isAssignableFrom(LinkedHashMap.class)) {
				if (mapClass.isAssignableFrom(HashMap.class)) {
					return new HashMap<>();
				}
				else {
					return new LinkedHashMap<>();
				}
			}
			else if (mapClass.isAssignableFrom(WeakHashMap.class)) {
				return new WeakHashMap<>();
			}
			else if (mapClass.isAssignableFrom(TreeMap.class)) {
				return new TreeMap<>();
			}
			else if (mapClass.isAssignableFrom(ConcurrentHashMap.class)) {
				return new ConcurrentHashMap<>();
			}
			
			// As a Last Resort, try to instantiate whatever Map is this by calling it's no-args constructor...
			return (Map<String, Object>) new Mirror().on(mapClass).invoke().constructor().withoutArgs();
		}
		
		return null;
	}
	
	/**
	 * Takes a {@code path.to.something.separated.by.dots} and set it into a {@link HashMap}. <strong>IT OVERRRIDES WHATEVER VALUE IS ALREADY ON THE MAP</strong>.
	 * @param rootMapClass The kind of {@link Map} that should be created
	 * @param path The path that should be set ({@code dot.path.to.target})
	 * @param value The value to set
	 * 
	 * @return The newly created {@link Map}
	 * @see #mapForClass(Class)
	 */
	private Map<String, Object> put(final Class<?> rootMapClass, Object currentMap, final String path, final Object value) {
		// Is the current map null or "not a map"? If so, create a new map so we can populate it
		if (currentMap == null || !(currentMap instanceof Map)) {
			// If currentMap is set but isn't a Map we'll override it with a Map so we can complete the dot path
			currentMap = mapForClass(rootMapClass);
		}
		
		final Map<String, Object> map = (Map<String, Object>) currentMap;
		final int indexOfDot = path.indexOf(".");
		final String currentPart = indexOfDot >= 0 ? path.substring(0, indexOfDot) : path;
		
		if (log.isDebugEnabled()) { log.debug("currentMap = {}, path = {}, indexOfDot = {}, currentPart = {}", map, path, indexOfDot, currentPart); }
		
		if (path.indexOf(".", indexOfDot) >= 0) {
			if (log.isDebugEnabled()) { log.debug("We still have levels to go. Invoking pathToMap({}, <current map>, \"{}\", {})...", rootMapClass, path.substring(currentPart.length() + 1), value); }
			map.put(currentPart, put(rootMapClass, map.get(currentPart), path.substring(currentPart.length() + 1), value));
		}
		else {
			if (log.isDebugEnabled()) { log.debug("Reached the path's end. Setting {} = {}", currentPart, value); }
			map.put(currentPart, value);
		}
		
		return map;
	}
}
