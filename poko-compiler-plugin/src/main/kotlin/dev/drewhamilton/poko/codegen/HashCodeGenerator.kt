package dev.drewhamilton.poko.codegen

import org.jetbrains.kotlin.codegen.AsmUtil
import org.jetbrains.kotlin.codegen.ClassBuilder
import org.jetbrains.kotlin.codegen.context.FieldOwnerContext
import org.jetbrains.kotlin.codegen.context.MethodContext
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.org.objectweb.asm.Label
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter

internal class HashCodeGenerator(
    declaration: KtClassOrObject,
    classDescriptor: ClassDescriptor,
    classAsmType: Type,
    fieldOwnerContext: FieldOwnerContext<*>,
    v: ClassBuilder,
    generationState: GenerationState
) : FunctionGenerator(declaration, classDescriptor, classAsmType, fieldOwnerContext, v, generationState) {

    override val methodDesc: String
        get() = "(${firstParameterDesc})I"

    override fun generateBytecode(
        instructionAdapter: InstructionAdapter,
        function: FunctionDescriptor,
        properties: List<PropertyDescriptor>,
        context: MethodContext,
        methodName: String
    ) {
        for (property in properties) {
            val l0 = Label()
            val l1 = Label()

            // Bytecode: Load the property's value
            //  ALOAD 0
            //  GETFIELD path/ClassName.property : <type>
            val type = genOrLoadOnStack(instructionAdapter, context, property, 0)

            val asmType = type.type
            if (!AsmUtil.isPrimitive(asmType)) {
                // Bytecode: Duplicate the property value and jump to L0 if it's null
                //  DUP
                //  IFNULL L0
                instructionAdapter.dup()
                instructionAdapter.ifnull(l0)

                if (asmType.sort == Type.ARRAY) {
                    val elementType = AsmUtil.correctElementType(asmType)
                    val elementDescriptor = if (AsmUtil.isPrimitive(elementType))
                        asmType.descriptor
                    else
                        "[Ljava/lang/Object;"
                    // Bytecode: Call Arrays.hashCode
                    //  INVOKESTATIC java/util/Arrays.hashCode (<element type>)I
                    instructionAdapter.invokestatic(
                        "java/util/Arrays", "hashCode",
                        "($elementDescriptor)I",
                        false
                    )
                } else {
                    // Bytecode: Call property.hashCode
                    //  INVOKEVIRTUAL java/lang/Object.hashCode ()I
                    instructionAdapter.invokevirtual("java/lang/Object", "hashCode", "()I", false)
                }
                // Bytecode: Go to L1
                //  GOTO L1
                instructionAdapter.goTo(l1)

                // Bytecode L0: property is null, load 0 onto the stack
                //  POP
                //  ICONST_0
                instructionAdapter.visitLabel(l0)
                instructionAdapter.pop()
                instructionAdapter.iconst(0)

                // Bytecode L1
                instructionAdapter.visitLabel(l1)
            } else if (asmType.sort == Type.FLOAT) {
                // Bytecode: Convert the float to a hashed int
                //  INVOKESTATIC java/lang/Float.floatToIntBits (F)I
                instructionAdapter.invokestatic("java/lang/Float", "floatToIntBits", "(F)I", false)
            } else if (asmType.sort == Type.DOUBLE || asmType.sort == Type.LONG) {
                if (asmType.sort == Type.DOUBLE) {
                    // Bytecode: Convert the double to a long
                    //  INVOKESTATIC java/lang/Double.doubleToLongBits (D)J
                    instructionAdapter.invokestatic("java/lang/Double", "doubleToLongBits", "(D)J", false)
                }

                // Bytecode: Convert the long to a hashed int
                //  DUP2
                //  BIPUSH 32
                //  LUSHR
                //  LXOR
                //  L2I
                instructionAdapter.dup2()
                instructionAdapter.iconst(32)
                instructionAdapter.ushr(Type.LONG_TYPE)
                instructionAdapter.xor(Type.LONG_TYPE)
                instructionAdapter.cast(Type.LONG_TYPE, Type.INT_TYPE)
            }

            if (property !== properties.first()) {
                // Bytecode: Add this to the hashCode so far
                //  IADD
                instructionAdapter.add(Type.INT_TYPE)
            }

            if (property == properties.last()) {
                // Bytecode: Return
                //  IRETURN
                instructionAdapter.areturn(Type.INT_TYPE)
            } else {
                // Bytecode L1: push 31 to the stack and multiply the property's hashCode by that
                //  BIPUSH 31
                //  IMUL
                instructionAdapter.iconst(31)
                instructionAdapter.mul(Type.INT_TYPE)
            }
        }
    }
}
