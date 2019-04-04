package name.martingeisse.mahdl.plugin;

import com.intellij.psi.PsiFile;
import com.intellij.util.indexing.*;
import com.intellij.util.io.IOUtil;
import com.intellij.util.io.KeyDescriptor;
import com.intellij.util.text.CaseInsensitiveStringHashingStrategy;
import name.martingeisse.mahdl.plugin.input.psi.Module;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 *
 */
public class MahdlModuleIndex extends ScalarIndexExtension<String> {

	@NonNls
	public static final ID<String, Void> NAME = ID.create(MahdlModuleIndex.class.getSimpleName());

	@NotNull
	@Override
	public ID<String, Void> getName() {
		return NAME;
	}

	@NotNull
	@Override
	public FileBasedIndex.InputFilter getInputFilter() {
		return file -> file.getName().endsWith(".mahdl");
	}

	@Override
	public boolean dependsOnFileContent() {
		return true;
	}

	@NotNull
	@Override
	public DataIndexer<String, Void, FileContent> getIndexer() {
		return fileContent -> {
			Map<String, Void> map = new HashMap<>();
			PsiFile psiFile = fileContent.getPsiFile();
			if (psiFile instanceof MahdlSourceFile) {
				Module module = ((MahdlSourceFile) psiFile).getModule();
				if (module != null) {
					map.put(module.getName(), null);
				}
			}
			return map;
		};
	}

	@NotNull
	@Override
	public KeyDescriptor<String> getKeyDescriptor() {
		return new ToStringDescriptor();
	}

	@Override
	public int getVersion() {
		return 0;
	}

	private static class ToStringDescriptor implements KeyDescriptor<String> {

		public int getHashCode(String value) {
			return value.hashCode();
		}

		public boolean isEqual(String s1, String s2) {
			return s1.equals(s2);
		}

		public void save(@NotNull DataOutput out, String value) throws IOException {
			IOUtil.writeUTF(out, value);
		}

		public String read(@NotNull DataInput in) throws IOException {
			return IOUtil.readUTF(in);
		}

	}

}
