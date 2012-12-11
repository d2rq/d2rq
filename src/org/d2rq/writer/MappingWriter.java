package org.d2rq.writer;

import java.io.OutputStream;
import java.io.Writer;

public interface MappingWriter {

	void write(OutputStream outStream);

	void write(Writer outWriter);

}