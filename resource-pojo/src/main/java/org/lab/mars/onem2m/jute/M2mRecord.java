package org.lab.mars.onem2m.jute;

import java.io.IOException;
import java.io.Serializable;

public interface M2mRecord extends Serializable {
	 void serialize(M2mOutputArchive archive, String tag) throws IOException;

	 void deserialize(M2mInputArchive archive, String tag)
			throws IOException;

}
