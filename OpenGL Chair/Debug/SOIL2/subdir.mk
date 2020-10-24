################################################################################
# Automatically-generated file. Do not edit!
################################################################################

# Add inputs and outputs from these tool invocations to the build variables 
C_SRCS += \
../SOIL2/SOIL2.c \
../SOIL2/image_DXT.c \
../SOIL2/image_helper.c \
../SOIL2/wfETC.c 

OBJS += \
./SOIL2/SOIL2.o \
./SOIL2/image_DXT.o \
./SOIL2/image_helper.o \
./SOIL2/wfETC.o 

C_DEPS += \
./SOIL2/SOIL2.d \
./SOIL2/image_DXT.d \
./SOIL2/image_helper.d \
./SOIL2/wfETC.d 


# Each subdirectory must supply rules for building sources it contributes
SOIL2/%.o: ../SOIL2/%.c
	@echo 'Building file: $<'
	@echo 'Invoking: GCC C Compiler'
	gcc -O0 -g3 -Wall -c -fmessage-length=0 -MMD -MP -MF"$(@:%.o=%.d)" -MT"$(@)" -o "$@" "$<"
	@echo 'Finished building: $<'
	@echo ' '


