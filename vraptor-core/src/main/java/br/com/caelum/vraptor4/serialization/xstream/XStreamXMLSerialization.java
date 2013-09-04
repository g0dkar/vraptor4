/***
 * Copyright (c) 2009 Caelum - www.caelum.com.br/opensource All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package br.com.caelum.vraptor4.serialization.xstream;

import java.io.IOException;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.servlet.http.HttpServletResponse;

import br.com.caelum.vraptor4.interceptor.TypeNameExtractor;
import br.com.caelum.vraptor4.serialization.ProxyInitializer;
import br.com.caelum.vraptor4.serialization.Serializer;
import br.com.caelum.vraptor4.serialization.SerializerBuilder;
import br.com.caelum.vraptor4.serialization.XMLSerialization;
import br.com.caelum.vraptor4.view.ResultException;

import com.thoughtworks.xstream.XStream;

/**
 * XStream implementation for XmlSerialization
 *
 * @author Lucas Cavalcanti
 * @since 3.0.2
 */
@RequestScoped
public class XStreamXMLSerialization implements XMLSerialization {

	private HttpServletResponse response;
	private TypeNameExtractor extractor;
	private ProxyInitializer initializer;
	private XStreamBuilder builder;

	@Deprecated // CDI eyes only
	public XStreamXMLSerialization() {}

	@Inject
	public XStreamXMLSerialization(HttpServletResponse response, TypeNameExtractor extractor, ProxyInitializer initializer, XStreamBuilder builder) {
		this.response = response;
		this.extractor = extractor;
		this.initializer = initializer;
		this.builder = builder;
	}

	public boolean accepts(String format) {
		return "xml".equals(format);
	}

	public <T> Serializer from(T object) {
		response.setContentType("application/xml");
		return getSerializer().from(object);
	}

	protected SerializerBuilder getSerializer() {
		try {
			return new XStreamSerializer(getXStream(), response.getWriter(), extractor, initializer);
		} catch (IOException e) {
			throw new ResultException("Unable to serialize data", e);
		}
	}

	public <T> Serializer from(T object, String alias) {
		response.setContentType("application/xml");
		return getSerializer().from(object, alias);
	}

	/**
	 * You can override this method for configuring XStream before serialization
     *
     * @deprecated prefer overwriting XStreamBuilderImpl
     * @return a configured instance of xstream
	 */
	@Deprecated
	protected XStream getXStream() {
		return builder.xmlInstance();
	}

}
