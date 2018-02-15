package me.lpk.antis.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import me.lpk.analysis.InsnValue;
import me.lpk.analysis.Sandbox;
import me.lpk.analysis.StackFrame;
import me.lpk.analysis.StackUtil;
import me.lpk.antis.AntiBase;
import me.lpk.util.OpUtils;

public class AntiDashO extends AntiBase {
	private boolean complex;

	public AntiDashO(Map<String, ClassNode> nodes) {
		super(nodes);
	}

	@Override
	public ClassNode scan(ClassNode node) {
		for (MethodNode mnode : node.methods) {
			if (complex) {
				replace_complex(mnode);
			} else {
				repl_gay(mnode);
			}
		}
		return node;
	}

	private void repl_gay(MethodNode method) {
		if (method.instructions.size() <= 3) {
			return;
		}
		// Fixing Flow
		AbstractInsnNode ain = method.instructions.getFirst();
		while (true) {
			if (ain == null || ain.getNext() == null) {
				break;
			}
			AbstractInsnNode next = ain.getNext();

			if ((ain instanceof LdcInsnNode) && next.getOpcode() == Opcodes.GOTO) {
				JumpInsnNode jin = (JumpInsnNode) next;
				AbstractInsnNode num = jin.label.getNext();
				if (num != null && !OpUtils.isNumeric(num)) {
					num = num.getNext();
				}
				// System.err.println("\t" + OpUtils.toString(num));
				if (num != null && OpUtils.isNumeric(num) && num.getNext().getOpcode() == Opcodes.INVOKESTATIC) {
					LdcInsnNode ldc = (LdcInsnNode) ain;
					MethodInsnNode min = (MethodInsnNode) num.getNext();
					String desc = min.desc;
					if (isDashDesc(desc)) {
						int mod = OpUtils.getIntValue(num);
						String text = ldc.cst.toString();
						String decoded = decode(text, mod);
						System.err.println("AAA: " + method.owner + "." + method.name + method.desc);
						if (checkDecoded(decoded)) {
							LdcInsnNode newLdc = new LdcInsnNode(decoded);
							method.instructions.remove(ldc);
							method.instructions.remove(num);
							method.instructions.set(min, newLdc);
						}
					}
				}
			}
			ain = ain.getNext();
		}
		// Fixing Strings
		ain = method.instructions.getFirst();
		while (true) {
			if (ain == null || ain.getNext() == null || ain.getNext().getNext() == null) {
				break;
			}
			if (ain instanceof LdcInsnNode && OpUtils.isNumeric(ain.getNext()) && ain.getNext().getNext().getOpcode() == Opcodes.INVOKESTATIC) {
				LdcInsnNode ldc = (LdcInsnNode) ain;
				AbstractInsnNode num = ain.getNext();
				MethodInsnNode min = (MethodInsnNode) ain.getNext().getNext();
				String desc = min.desc;
				if (isDashDesc(desc)) {
					int mod = OpUtils.getIntValue(num);
					String text = ldc.cst.toString();
					String decoded = decode(text, mod);
					if (checkDecoded(decoded)) {
						LdcInsnNode newLdc = new LdcInsnNode(decoded);
						method.instructions.remove(ldc);
						method.instructions.remove(num);
						method.instructions.set(min, newLdc);
						ain = newLdc;
					}
				}
			}
			ain = ain.getNext();
		}
	}

	private boolean checkDecoded(String decoded) {
		if (decoded == null)
			return false;
		for (char c : decoded.toCharArray()) {
			if (c == 9 || c == 10) {
				// whitespace
				continue;
			}
			if (c < 20 || c > 126) {
				// normal ascii
				return false;
			}
		}
		return true;
	}

	/**
	 * Iterates through Insns in a method. If a certain pattern matching DashO
	 * usage is met, the insns are reformatted to only contain the output
	 * string.
	 * 
	 * @param method
	 */
	private void replace_complex(MethodNode method) {
		StackFrame[] frames = StackUtil.getFrames(method);
		AbstractInsnNode ain = method.instructions.getFirst();
		List<String> strings = new ArrayList<String>();
		List<Integer> argSizes = new ArrayList<Integer>();
		List<Integer> indecies = new ArrayList<Integer>();
		while (ain != null) {
			if (ain.getOpcode() == Opcodes.INVOKESTATIC) {
				String desc = ((MethodInsnNode) ain).desc;
				if (isDashDesc(desc)) {
					int opIndex = OpUtils.getIndex(ain);
					Type t = Type.getMethodType(desc);
					MethodInsnNode min = (MethodInsnNode) ain;
					ClassNode owner = getNodes().get(min.owner);
					Object[] args = new Object[t.getArgumentTypes().length];
					// DashO always has at least 2 args.
					if (opIndex < 0 || opIndex >= frames.length || args.length <= 1) {
						ain = ain.getNext();
						continue;
					}
					StackFrame frame = frames[opIndex];
					if (frame == null) {
						ain = ain.getNext();
						continue;
					}
					if (frame.getStackSize() < args.length) {
						// This should never happen unless there's some weird
						// jump/flow obfuscation.
						ain = ain.getNext();
						continue;
					}
					boolean failed = false;
					for (int i = 0; i < args.length; i++) {
						InsnValue val = (InsnValue) frame.getStack(frame.getStackSize() - i - 1);
						if (val.getValue() == null) {
							failed = true;
							break;
						}
						args[args.length - i - 1] = val.getValue();
					}
					if (failed) {
						ain = ain.getNext();
						continue;
					}
					Object o = Sandbox.getIsolatedReturn(owner, min, args);
					if (o != null) {
						strings.add(o.toString());
						argSizes.add(args.length);
						indecies.add(opIndex);
					}
				}
			}
			ain = ain.getNext();
		}
		ain = method.instructions.getFirst();
		int offset = 0;
		while (ain != null) {
			if (ain.getOpcode() == Opcodes.INVOKESTATIC) {
				MethodInsnNode min = (MethodInsnNode) ain;
				if (isDashDesc(min.desc)) {
					int opIndex = OpUtils.getIndex(ain);
					if (indecies.size() > 0 && indecies.get(0) + offset == opIndex) {
						indecies.remove(0);
						int args = argSizes.remove(0);
						String string = strings.remove(0);
						for (int i = 0; i < args; i++) {
							method.instructions.insertBefore(min, new InsnNode(Opcodes.POP));
							offset++;
						}
						LdcInsnNode ldc = new LdcInsnNode(string);
						method.instructions.set(ain, ldc);
						ain = ldc;
					}
				}
			}
			ain = ain.getNext();
		}
	}

	private boolean isDashDesc(String desc) {
		// String s = "Ljava/lang/String;";
		// return desc.endsWith(s) && desc.replace("I", "").replace(s,
		// "").length() == 2;
		return desc.equals("(Ljava/lang/String;I)Ljava/lang/String;");
	}

	public static String decode(final String s, int n) {
		try {
			final int n2 = 4;
			final int n3 = 1 + n2;
			final boolean b = false;
			final char[] charArray = s.toCharArray();
			final int length = charArray.length;
			final char[] array = charArray;
			int n4 = b ? 1 : 0;
			final int n5 = (n2 << n3) - 1 ^ 0x20;
			char[] array2;
			while (true) {
				array2 = array;
				if (n4 == length) {
					break;
				}
				final int n6 = n4;
				final int n7 = array2[n6] ^ (n & n5);
				++n;
				++n4;
				array2[n6] = (char) n7;
			}
			return String.valueOf(array2, 0, length).intern();
		} catch (Exception e) {
			return null;
		}
	}
}
