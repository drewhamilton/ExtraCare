package dev.drewhamilton.careful.codegen

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
            if (AsmUtil.isPrimitive(type.type)) {
                // Bytecode: Add the primitive
                //  IADD
                instructionAdapter.add(Type.INT_TYPE)

                // TODO: Long, Array
            } else {
                // Bytecode: Duplicate the property value and jump to L0 if it's null
                //  DUP
                //  IFNULL L0
                instructionAdapter.dup()
                instructionAdapter.ifnull(l0)

                // Bytecode: Call hashCode() and jump to L1
                instructionAdapter.invokevirtual("java/lang/Object", "hashCode", "()I", false)
                instructionAdapter.goTo(l1)

                // Bytecode L0: property is null, load 0 onto the stack
                instructionAdapter.visitLabel(l0)
                instructionAdapter.pop()
                instructionAdapter.iconst(0)
            }

            // Bytecode L1: push 31 to the stack and multiply the property's hashCode by that
            instructionAdapter.iconst(31)
            instructionAdapter.mul(Type.INT_TYPE)
        }
    }
}
