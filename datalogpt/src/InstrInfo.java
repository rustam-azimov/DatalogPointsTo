package datalogpt.src;

public class InstrInfo {
    public final int index;
    public final String insn;
    public final String methodId;

    public InstrInfo(String methodId, String insn, int index) {
        this.methodId = methodId;
        this.index = index;
        this.insn = insn;
    }

    public InstrInfo(String methodId, String kind, SessionCounter session) {
        this(methodId, Representation.instructionId(methodId, kind, session.nextNumber(kind)), session.calcInstructionIndex(new Object()));
    }

    public String toString() {
        return "InstrInfo{ index=" + this.index + ", insn=" + this.insn + ", methodId=" + this.methodId + " }";
    }
}
