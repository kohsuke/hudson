/**
 * 
 */
package hudson.cli;

import org.apache.commons.codec.digest.DigestUtils;

public class Cookie {
	static final String DUMMY_PASSWORD = "N/A";
	
	public final long tokenExpiryTime;
	public final String user;

	public Cookie(String user, long tokenExpiryTime) {
		this.user = user;
		this.tokenExpiryTime = tokenExpiryTime;
	}

	public String getSignature(String key) {
		return DigestUtils.md5Hex(user + ":" + tokenExpiryTime + ":"
				+ DUMMY_PASSWORD + ":" + key);
	}

	public String encode(String key) {
		return user + ":" + tokenExpiryTime + ":" + getSignature(key);
	}

	public static Cookie decode(String key, String cookie)
			throws IllegalArgumentException {
		String[] cookieTokens = cookie.split(":");

		long tokenExpiryTime;

		String user = cookieTokens[0];
		String tokenExpiryTimeText = cookieTokens[1];
		String signature = cookieTokens[2];

		try {
			tokenExpiryTime = new Long(tokenExpiryTimeText).longValue();
		} catch (NumberFormatException nfe) {
			throw new IllegalArgumentException("tokenExpiryTime invalid: "
					+ tokenExpiryTimeText);
		}

		String expectedTokenSignature = new Cookie(user, tokenExpiryTime).getSignature(key);
		if (!expectedTokenSignature.equals(signature)) {
			throw new IllegalArgumentException("bad cookie!");
		}

		return new Cookie(user, tokenExpiryTime);
	}
}