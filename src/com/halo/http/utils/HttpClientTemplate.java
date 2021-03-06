package com.halo.http.utils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import com.halo.json.utils.JSONUtils;

public class HttpClientTemplate implements HttpTemplate {

	private final String JSON_CONTENT_TYPE = "application/json";

	private String getUrlWithArgs(String url, Map<String, String> args) throws HttpUtilsException {
		String params = "";
		for (String paramName : args.keySet()) {
			String paramValue;
			try {
				paramValue = URLEncoder.encode(args.get(paramName), "UTF-8");
			} catch (UnsupportedEncodingException e) {
				throw new HttpUtilsException("URL encode error.", e);
			}
			params += paramName + "=" + paramValue + "&";
		}
		if (params.length() > 0) {
			params = params.substring(0, params.length() - 1);
		}

		String urlWithArgs = url;
		if (!params.isEmpty()) {
			urlWithArgs += "?" + params;
		}
		return urlWithArgs;
	}

	private File saveStreamToFile(InputStream stream) throws HttpUtilsException {
		UUID uuid = new UUID(ThreadLocalRandom.current().nextLong(), ThreadLocalRandom.current().nextLong());
		File downloadFile = new File(uuid.toString() + ".dld");
		try {
			downloadFile.createNewFile();
		} catch (IOException e) {
			throw new HttpUtilsException("Can't create new download file. ", e);
		}

		OutputStream outputStream = null;
		try {
			outputStream = new FileOutputStream(downloadFile);

			int size = 0;
			byte[] bytes = new byte[1024];
			BufferedInputStream bufferedInputStream = new BufferedInputStream(stream);
			while ((size = bufferedInputStream.read(bytes)) != -1) {
				outputStream.write(bytes, 0, size);
			}
		} catch (FileNotFoundException e) {
			throw new HttpUtilsException("Can't create a FileOutputStream.", e);
		} catch (IOException e) {
			throw new HttpUtilsException("Can't read from input stream or write to output stream.", e);
		} finally {
			try {
				if (null != outputStream) {
					outputStream.flush();
					outputStream.close();
				}
			} catch (IOException e) {
				throw new HttpUtilsException("Close stream error.", e);
			}
		}

		return downloadFile;
	}

	@Override
	public String get(String url, Map<String, String> args) throws HttpUtilsException {
		String urlWithArgs = getUrlWithArgs(url, args);

		CloseableHttpClient httpClient = HttpClients.createDefault();
		HttpGet httpGet = new HttpGet(urlWithArgs);

		// Create a custom response handler
		ResponseHandler<String> responseHandler = new ResponseHandler<String>() {

			@Override
			public String handleResponse(final HttpResponse response) throws ClientProtocolException, IOException {
				int status = response.getStatusLine().getStatusCode();
				if (status >= 200 && status < 300) {
					HttpEntity entity = response.getEntity();
					return entity != null ? EntityUtils.toString(entity, "UTF-8") : null;
				} else {
					throw new ClientProtocolException("Unexpected response status: " + status);
				}
			}

		};
		String responseBody = null;
		try {
			responseBody = httpClient.execute(httpGet, responseHandler);
		} catch (IOException e) {
			throw new HttpUtilsException("Get from " + urlWithArgs + " error.", e);
		} finally {
			try {
				httpClient.close();
			} catch (IOException e) {
				throw new HttpUtilsException("Close http stream error.", e);
			}
		}

		return responseBody;
	}

	@Override
	public File download(String url, Map<String, String> args) throws HttpUtilsException {
		String urlWithArgs = getUrlWithArgs(url, args);

		CloseableHttpClient httpClient = HttpClients.createDefault();
		HttpGet httpGet = new HttpGet(urlWithArgs);

		// Create a custom response handler
		ResponseHandler<File> responseHandler = new ResponseHandler<File>() {

			@Override
			public File handleResponse(final HttpResponse response) throws ClientProtocolException, IOException {
				int status = response.getStatusLine().getStatusCode();
				if (status >= 200 && status < 300) {
					HttpEntity entity = response.getEntity();
					if (entity == null) {
						throw new ClientProtocolException("There's no content in http response.");
					}
					try {
						return saveStreamToFile(entity.getContent());
					} catch (HttpUtilsException e) {
						throw new IOException("Save file error.", e);
					}
				} else {
					throw new ClientProtocolException("Unexpected response status: " + status);
				}
			}

		};

		File downloadFile = null;
		try {
			downloadFile = httpClient.execute(httpGet, responseHandler);
		} catch (IOException e) {
			throw new HttpUtilsException("Download from " + urlWithArgs + " error.", e);
		} finally {
			try {
				httpClient.close();
			} catch (IOException e) {
				throw new HttpUtilsException("Close http stream error.", e);
			}
		}

		return downloadFile;
	}

	@Override
	public String post(String url, Map<String, String> args, String requestBody, String contentType)
			throws HttpUtilsException {
		String urlWithArgs = getUrlWithArgs(url, args);

		CloseableHttpClient httpClient = HttpClients.createDefault();
		HttpPost httpPost = new HttpPost(urlWithArgs);
		StringEntity reqEntity = new StringEntity(requestBody, "UTF-8");
		reqEntity.setContentEncoding("UTF-8");
		if (null != contentType && !contentType.isEmpty()) {
			// application/json
			reqEntity.setContentType(contentType);
		}
		httpPost.setEntity(reqEntity);

		// Create a custom response handler
		ResponseHandler<String> responseHandler = new ResponseHandler<String>() {

			@Override
			public String handleResponse(final HttpResponse response) throws ClientProtocolException, IOException {
				int status = response.getStatusLine().getStatusCode();
				if (status >= 200 && status < 300) {
					HttpEntity entity = response.getEntity();
					return entity != null ? EntityUtils.toString(entity, "UTF-8") : null;
				} else {
					throw new ClientProtocolException("Unexpected response status: " + status);
				}
			}

		};
		String responseBody = null;
		try {
			responseBody = httpClient.execute(httpPost, responseHandler);
		} catch (IOException e) {
			throw new HttpUtilsException("Post to " + urlWithArgs + " error.", e);
		} finally {
			try {
				httpClient.close();
			} catch (IOException e) {
				throw new HttpUtilsException("Close http stream error.", e);
			}
		}

		return responseBody;
	}

	@Override
	public <P, R> R jsonPost(String url, Map<String, String> args, JSONUtils<P> postJsonUtils, P postJsonBean,
			JSONUtils<R> resultJsonUtils) throws HttpUtilsException {
		String jsonStr = postJsonUtils.getJsonStr(postJsonBean);
		String resultStr = this.post(url, args, jsonStr, JSON_CONTENT_TYPE);

		return resultJsonUtils.getJsonBean(resultStr);
	}

	@Override
	public File downloadUsePost(String url, Map<String, String> args, String requestBody, String contentType)
			throws HttpUtilsException {
		String urlWithArgs = getUrlWithArgs(url, args);

		CloseableHttpClient httpClient = HttpClients.createDefault();
		HttpPost httpPost = new HttpPost(urlWithArgs);
		StringEntity reqEntity = new StringEntity(requestBody, "UTF-8");
		reqEntity.setContentEncoding("UTF-8");
		if (null != contentType && !contentType.isEmpty()) {
			// application/json
			reqEntity.setContentType(contentType);
		}
		httpPost.setEntity(reqEntity);

		// Create a custom response handler
		ResponseHandler<File> responseHandler = new ResponseHandler<File>() {

			@Override
			public File handleResponse(final HttpResponse response) throws ClientProtocolException, IOException {
				int status = response.getStatusLine().getStatusCode();
				if (status >= 200 && status < 300) {
					HttpEntity entity = response.getEntity();
					if (entity == null) {
						throw new ClientProtocolException("There's no content in http response.");
					}
					try {
						return saveStreamToFile(entity.getContent());
					} catch (HttpUtilsException e) {
						throw new IOException("Save file error.", e);
					}
				} else {
					throw new ClientProtocolException("Unexpected response status: " + status);
				}
			}

		};

		File downloadFile = null;
		try {
			downloadFile = httpClient.execute(httpPost, responseHandler);
		} catch (IOException e) {
			throw new HttpUtilsException("Download from " + urlWithArgs + " error.", e);
		} finally {
			try {
				httpClient.close();
			} catch (IOException e) {
				throw new HttpUtilsException("Close http stream error.", e);
			}
		}

		return downloadFile;
	}

}
