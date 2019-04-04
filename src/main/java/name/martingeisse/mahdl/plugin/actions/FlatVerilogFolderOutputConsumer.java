package name.martingeisse.mahdl.plugin.actions;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.vfs.VirtualFile;
import name.martingeisse.mahdl.plugin.codegen.DesignVerilogGenerator;
import name.martingeisse.mahdl.plugin.util.UserMessageException;
import org.apache.commons.lang3.mutable.MutableObject;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;

/**
 *
 */
public class FlatVerilogFolderOutputConsumer implements DesignVerilogGenerator.OutputConsumer {

	private final VirtualFile folder;

	public FlatVerilogFolderOutputConsumer(VirtualFile folder) {
		this.folder = folder;
	}

	@Override
	public void consume(String fileName, String contents) throws Exception {
		MutableObject<Exception> exceptionHolder = new MutableObject<>();
		ApplicationManager.getApplication().runWriteAction(() -> {
			try {
				VirtualFile outputFile = folder.findChild(fileName);
				if (outputFile == null) {
					outputFile = folder.createChildData(this, fileName);
				} else if (outputFile.isDirectory()) {
					throw new UserMessageException("collision with existing folder while creating output file " + fileName + "'");
				}
				try (OutputStream outputStream = outputFile.getOutputStream(this)) {
					try (OutputStreamWriter outputStreamWriter = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8)) {
						outputStreamWriter.write(contents);
					}
				}
			} catch (IOException e) {
				exceptionHolder.setValue(e);
			}
		});
		if (exceptionHolder.getValue() != null) {
			throw exceptionHolder.getValue();
		}
	}

}
