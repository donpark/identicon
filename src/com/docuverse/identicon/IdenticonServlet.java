package com.docuverse.identicon;

import java.awt.image.RenderedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This servlet generates <i>identicon</i> (visual identifier) images ranging
 * from 16x16 to 64x64 in size.
 * 
 * <h5>Supported Image Formats</h5>
 * <p>
 * Currently only PNG is supported because <code>javax.imageio</code> package
 * does not come with built-in GIF encoder and PNG is the only remaining
 * reasonable format.
 * </p>
 * <h5>Initialization Parameters:</h5>
 * <blockquote>
 * <dl>
 * <dt>inetSalt</dt>
 * <dd>salt used to generate identicon code with. must be fairly long.
 * (Required)</dd>
 * <dt>cacheProvider</dt>
 * <dd>full class path to <code>IdenticonCache</code> implementation.
 * (Optional)</dd>
 * </dl>
 * </blockquote>
 * <h5>Request ParametersP</h5>
 * <blockquote>
 * <dl>
 * <dt>code</dt>
 * <dd>identicon code to render. If missing, requester's IP addresses is used
 * to generated one. (Optional)</dd>
 * <dt>size</dt>
 * <dd>identicon size in pixels. If missing, a 16x16 pixels identicon is
 * returned. Minimum size is 16 and maximum is 64. (Optional)</dd>
 * </dl>
 * </blockquote>
 * 
 * @author don
 */
public class IdenticonServlet extends HttpServlet {

	private static final long serialVersionUID = -3507466186902317988L;

	private static final Log log = LogFactory.getLog(IdenticonServlet.class);

	private static final String INIT_PARAM_VERSION = "version";

	private static final String INIT_PARAM_INET_SALT = "inetSalt";

	private static final String INIT_PARAM_CACHE_PROVIDER = "cacheProvider";

	private static final String PARAM_IDENTICON_SIZE = "size";

	private static final String PARAM_IDENTICON_SIZE_SHORT = "s";

	private static final String PARAM_IDENTICON_CODE = "code";

	private static final String PARAM_IDENTICON_CODE_SHORT = "c";

//	private static final String PARAM_IDENTICON_TYPE = "type";
//
//	private static final String PARAM_IDENTICON_TYPE_SHORT = "t";

	private static final String IDENTICON_IMAGE_FORMAT = "PNG";

	private static final String IDENTICON_IMAGE_MIMETYPE = "image/png";

	private static final long DEFAULT_IDENTICON_EXPIRES_IN_MILLIS = 24 * 60 * 60 * 1000;

	private int version = 1;

	private IdenticonRenderer renderer = new NineBlockIdenticonRenderer2();

	private IdenticonCache cache;

	private long identiconExpiresInMillis = DEFAULT_IDENTICON_EXPIRES_IN_MILLIS;

	@Override
	public void init(ServletConfig cfg) throws ServletException {
		super.init(cfg);

		// Since identicons cache expiration is very long, version is
		// used in ETag to force identicons to be updated as needed.
		// Change veresion whenever rendering codes changes result in
		// visual changes.
		if (cfg.getInitParameter(INIT_PARAM_VERSION) != null)
			this.version = Integer.parseInt(cfg
					.getInitParameter(INIT_PARAM_VERSION));

		String inetSalt = cfg.getInitParameter(INIT_PARAM_INET_SALT);
		if (inetSalt != null && inetSalt.length() > 0)
			IdenticonUtil.setInetSalt(inetSalt);
		else
			throw new ServletException(INIT_PARAM_INET_SALT
					+ " init parameter not set");

		String cacheProvider = cfg.getInitParameter(INIT_PARAM_CACHE_PROVIDER);
		if (cacheProvider != null) {
			try {
				Class cacheClass = Class.forName(cacheProvider);
				this.cache = (IdenticonCache) cacheClass.newInstance();
			} catch (Exception e) {
				log.error(e);
			}
		}
	}

	@Override
	protected void doGet(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {

		String codeParam = request.getParameter(PARAM_IDENTICON_CODE_SHORT);
		if (codeParam == null)
			codeParam = request.getParameter(PARAM_IDENTICON_CODE);
		boolean codeSpecified = codeParam != null && codeParam.length() > 0;
		int code = IdenticonUtil.getIdenticonCode(codeParam, request
				.getRemoteAddr());

		String sizeParam = request.getParameter(PARAM_IDENTICON_SIZE_SHORT);
		if (sizeParam == null)
			sizeParam =request.getParameter(PARAM_IDENTICON_SIZE);
		int size = IdenticonUtil.getIdenticonSize(sizeParam);

//		String typeParam = request.getParameter(PARAM_IDENTICON_TYPE_SHORT);
//		if (typeParam == null)
//			typeParam = request.getParameter(PARAM_IDENTICON_TYPE);

		String identiconETag = IdenticonUtil.getIdenticonETag(code, size,
				version);
		String requestETag = request.getHeader("If-None-Match");

		if (requestETag != null && requestETag.equals(identiconETag)) {
			response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
		} else {
			byte[] imageBytes = null;

			// retrieve image bytes from either cache or renderer
			if (cache == null
					|| (imageBytes = cache.get(identiconETag)) == null) {
				ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
				RenderedImage image = renderer.render(code, size);
				ImageIO.write(image, IDENTICON_IMAGE_FORMAT, byteOut);
				imageBytes = byteOut.toByteArray();
				if (cache != null)
					cache.add(identiconETag, imageBytes);
			}

			// set ETag and, if code was provided, Expires header
			response.setHeader("ETag", identiconETag);
			if (codeSpecified) {
				long expires = System.currentTimeMillis()
						+ identiconExpiresInMillis;
				response.addDateHeader("Expires", expires);
			}

			// return image bytes to requester
			response.setContentType(IDENTICON_IMAGE_MIMETYPE);
			response.setContentLength(imageBytes.length);
			response.getOutputStream().write(imageBytes);
		}
	}
}
