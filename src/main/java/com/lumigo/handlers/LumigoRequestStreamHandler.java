package com.lumigo.handlers;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.lumigo.core.SpansContainer;
import com.lumigo.core.configuration.LumigoConfiguration;
import com.lumigo.core.network.Reporter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public abstract class LumigoRequestStreamHandler implements RequestStreamHandler {

  private static final Logger LOG = LogManager.getLogger(LumigoRequestStreamHandler.class);

  @Override
  public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context)
      throws IOException {
    try {
      LOG.debug("Start {} Lumigo tracer", LumigoRequestStreamHandler.class.getName());
      SpansContainer.getInstance().init(System.getenv(), context, null);
      SpansContainer.getInstance().start();
      Reporter.reportSpans(SpansContainer.getInstance().getStartFunctionSpan());
      doHandleRequest(inputStream, outputStream, context);
      SpansContainer.getInstance().end();
    } catch (Throwable e) {
      LOG.debug("Customer lambda had exception {}", e.getClass().getName());
      SpansContainer.getInstance().endWithException(e);
      throw e;
    } finally {
      Reporter.reportSpans(SpansContainer.getInstance().getAllCollectedSpans());
    }
  }

  public abstract void doHandleRequest(
      InputStream inputStream, OutputStream outputStream, Context context) throws IOException;
}
