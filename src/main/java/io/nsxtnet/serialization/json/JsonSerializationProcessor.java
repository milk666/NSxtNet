/*
    Copyright 2013, Strategic Gains, Inc.

	Licensed under the Apache License, Version 2.0 (the "License");
	you may not use this file except in compliance with the License.
	You may obtain a copy of the License at

		http://www.apache.org/licenses/LICENSE-2.0

	Unless required by applicable law or agreed to in writing, software
	distributed under the License is distributed on an "AS IS" BASIS,
	WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
	See the License for the specific language governing permissions and
	limitations under the License.
*/
package io.nsxtnet.serialization.json;

import io.nsxtnet.contenttype.ContentType;
import io.nsxtnet.contenttype.MediaTypeParser;
import io.nsxtnet.serialization.AbstractSerializationProcessor;
import io.nsxtnet.util.Format;
import io.nsxtnet.util.StringUtils;

import java.util.Arrays;
import java.util.List;

/**
 * @author toddf
 * @since Jul 18, 2013
 */
public abstract class JsonSerializationProcessor extends AbstractSerializationProcessor
{
	private static final String SUPPORTED_MEDIA_TYPES = StringUtils.join(",",
			ContentType.JSON,
			ContentType.JAVASCRIPT,
			ContentType.TEXT_JAVASCRIPT);

	public JsonSerializationProcessor()
	{
		this(Format.JSON);
	}

	public JsonSerializationProcessor(String format)
	{
		this(Arrays.asList(format));
	}

	public JsonSerializationProcessor(List<String> supportedFormats)
	{
		super(supportedFormats, MediaTypeParser.parse(SUPPORTED_MEDIA_TYPES));
	}
}
