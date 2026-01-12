package family;

import static io.grpc.MethodDescriptor.generateFullMethodName;

/**
 */
@javax.annotation.Generated(
    value = "by gRPC proto compiler (version 1.67.1)",
    comments = "Source: family.proto")
@io.grpc.stub.annotations.GrpcGenerated
public final class FamilyServiceGrpc {

  private FamilyServiceGrpc() {}

  public static final java.lang.String SERVICE_NAME = "family.FamilyService";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<family.NodeInfo,
      family.FamilyView> getJoinMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "Join",
      requestType = family.NodeInfo.class,
      responseType = family.FamilyView.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<family.NodeInfo,
      family.FamilyView> getJoinMethod() {
    io.grpc.MethodDescriptor<family.NodeInfo, family.FamilyView> getJoinMethod;
    if ((getJoinMethod = FamilyServiceGrpc.getJoinMethod) == null) {
      synchronized (FamilyServiceGrpc.class) {
        if ((getJoinMethod = FamilyServiceGrpc.getJoinMethod) == null) {
          FamilyServiceGrpc.getJoinMethod = getJoinMethod =
              io.grpc.MethodDescriptor.<family.NodeInfo, family.FamilyView>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "Join"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  family.NodeInfo.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  family.FamilyView.getDefaultInstance()))
              .setSchemaDescriptor(new FamilyServiceMethodDescriptorSupplier("Join"))
              .build();
        }
      }
    }
    return getJoinMethod;
  }

  private static volatile io.grpc.MethodDescriptor<family.Empty,
      family.FamilyView> getGetFamilyMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "GetFamily",
      requestType = family.Empty.class,
      responseType = family.FamilyView.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<family.Empty,
      family.FamilyView> getGetFamilyMethod() {
    io.grpc.MethodDescriptor<family.Empty, family.FamilyView> getGetFamilyMethod;
    if ((getGetFamilyMethod = FamilyServiceGrpc.getGetFamilyMethod) == null) {
      synchronized (FamilyServiceGrpc.class) {
        if ((getGetFamilyMethod = FamilyServiceGrpc.getGetFamilyMethod) == null) {
          FamilyServiceGrpc.getGetFamilyMethod = getGetFamilyMethod =
              io.grpc.MethodDescriptor.<family.Empty, family.FamilyView>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "GetFamily"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  family.Empty.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  family.FamilyView.getDefaultInstance()))
              .setSchemaDescriptor(new FamilyServiceMethodDescriptorSupplier("GetFamily"))
              .build();
        }
      }
    }
    return getGetFamilyMethod;
  }

  private static volatile io.grpc.MethodDescriptor<family.ChatMessage,
      family.Empty> getReceiveChatMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "ReceiveChat",
      requestType = family.ChatMessage.class,
      responseType = family.Empty.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<family.ChatMessage,
      family.Empty> getReceiveChatMethod() {
    io.grpc.MethodDescriptor<family.ChatMessage, family.Empty> getReceiveChatMethod;
    if ((getReceiveChatMethod = FamilyServiceGrpc.getReceiveChatMethod) == null) {
      synchronized (FamilyServiceGrpc.class) {
        if ((getReceiveChatMethod = FamilyServiceGrpc.getReceiveChatMethod) == null) {
          FamilyServiceGrpc.getReceiveChatMethod = getReceiveChatMethod =
              io.grpc.MethodDescriptor.<family.ChatMessage, family.Empty>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "ReceiveChat"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  family.ChatMessage.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  family.Empty.getDefaultInstance()))
              .setSchemaDescriptor(new FamilyServiceMethodDescriptorSupplier("ReceiveChat"))
              .build();
        }
      }
    }
    return getReceiveChatMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static FamilyServiceStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<FamilyServiceStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<FamilyServiceStub>() {
        @java.lang.Override
        public FamilyServiceStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new FamilyServiceStub(channel, callOptions);
        }
      };
    return FamilyServiceStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static FamilyServiceBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<FamilyServiceBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<FamilyServiceBlockingStub>() {
        @java.lang.Override
        public FamilyServiceBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new FamilyServiceBlockingStub(channel, callOptions);
        }
      };
    return FamilyServiceBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static FamilyServiceFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<FamilyServiceFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<FamilyServiceFutureStub>() {
        @java.lang.Override
        public FamilyServiceFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new FamilyServiceFutureStub(channel, callOptions);
        }
      };
    return FamilyServiceFutureStub.newStub(factory, channel);
  }

  /**
   */
  public interface AsyncService {

    /**
     */
    default void join(family.NodeInfo request,
        io.grpc.stub.StreamObserver<family.FamilyView> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getJoinMethod(), responseObserver);
    }

    /**
     */
    default void getFamily(family.Empty request,
        io.grpc.stub.StreamObserver<family.FamilyView> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getGetFamilyMethod(), responseObserver);
    }

    /**
     */
    default void receiveChat(family.ChatMessage request,
        io.grpc.stub.StreamObserver<family.Empty> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getReceiveChatMethod(), responseObserver);
    }
  }

  /**
   * Base class for the server implementation of the service FamilyService.
   */
  public static abstract class FamilyServiceImplBase
      implements io.grpc.BindableService, AsyncService {

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return FamilyServiceGrpc.bindService(this);
    }
  }

  /**
   * A stub to allow clients to do asynchronous rpc calls to service FamilyService.
   */
  public static final class FamilyServiceStub
      extends io.grpc.stub.AbstractAsyncStub<FamilyServiceStub> {
    private FamilyServiceStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected FamilyServiceStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new FamilyServiceStub(channel, callOptions);
    }

    /**
     */
    public void join(family.NodeInfo request,
        io.grpc.stub.StreamObserver<family.FamilyView> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getJoinMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void getFamily(family.Empty request,
        io.grpc.stub.StreamObserver<family.FamilyView> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getGetFamilyMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void receiveChat(family.ChatMessage request,
        io.grpc.stub.StreamObserver<family.Empty> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getReceiveChatMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   * A stub to allow clients to do synchronous rpc calls to service FamilyService.
   */
  public static final class FamilyServiceBlockingStub
      extends io.grpc.stub.AbstractBlockingStub<FamilyServiceBlockingStub> {
    private FamilyServiceBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected FamilyServiceBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new FamilyServiceBlockingStub(channel, callOptions);
    }

    /**
     */
    public family.FamilyView join(family.NodeInfo request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getJoinMethod(), getCallOptions(), request);
    }

    /**
     */
    public family.FamilyView getFamily(family.Empty request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getGetFamilyMethod(), getCallOptions(), request);
    }

    /**
     */
    public family.Empty receiveChat(family.ChatMessage request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getReceiveChatMethod(), getCallOptions(), request);
    }
  }

  /**
   * A stub to allow clients to do ListenableFuture-style rpc calls to service FamilyService.
   */
  public static final class FamilyServiceFutureStub
      extends io.grpc.stub.AbstractFutureStub<FamilyServiceFutureStub> {
    private FamilyServiceFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected FamilyServiceFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new FamilyServiceFutureStub(channel, callOptions);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<family.FamilyView> join(
        family.NodeInfo request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getJoinMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<family.FamilyView> getFamily(
        family.Empty request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getGetFamilyMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<family.Empty> receiveChat(
        family.ChatMessage request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getReceiveChatMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_JOIN = 0;
  private static final int METHODID_GET_FAMILY = 1;
  private static final int METHODID_RECEIVE_CHAT = 2;

  private static final class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final AsyncService serviceImpl;
    private final int methodId;

    MethodHandlers(AsyncService serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_JOIN:
          serviceImpl.join((family.NodeInfo) request,
              (io.grpc.stub.StreamObserver<family.FamilyView>) responseObserver);
          break;
        case METHODID_GET_FAMILY:
          serviceImpl.getFamily((family.Empty) request,
              (io.grpc.stub.StreamObserver<family.FamilyView>) responseObserver);
          break;
        case METHODID_RECEIVE_CHAT:
          serviceImpl.receiveChat((family.ChatMessage) request,
              (io.grpc.stub.StreamObserver<family.Empty>) responseObserver);
          break;
        default:
          throw new AssertionError();
      }
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public io.grpc.stub.StreamObserver<Req> invoke(
        io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        default:
          throw new AssertionError();
      }
    }
  }

  public static final io.grpc.ServerServiceDefinition bindService(AsyncService service) {
    return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
        .addMethod(
          getJoinMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              family.NodeInfo,
              family.FamilyView>(
                service, METHODID_JOIN)))
        .addMethod(
          getGetFamilyMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              family.Empty,
              family.FamilyView>(
                service, METHODID_GET_FAMILY)))
        .addMethod(
          getReceiveChatMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              family.ChatMessage,
              family.Empty>(
                service, METHODID_RECEIVE_CHAT)))
        .build();
  }

  private static abstract class FamilyServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    FamilyServiceBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return family.FamilyProto.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("FamilyService");
    }
  }

  private static final class FamilyServiceFileDescriptorSupplier
      extends FamilyServiceBaseDescriptorSupplier {
    FamilyServiceFileDescriptorSupplier() {}
  }

  private static final class FamilyServiceMethodDescriptorSupplier
      extends FamilyServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final java.lang.String methodName;

    FamilyServiceMethodDescriptorSupplier(java.lang.String methodName) {
      this.methodName = methodName;
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.MethodDescriptor getMethodDescriptor() {
      return getServiceDescriptor().findMethodByName(methodName);
    }
  }

  private static volatile io.grpc.ServiceDescriptor serviceDescriptor;

  public static io.grpc.ServiceDescriptor getServiceDescriptor() {
    io.grpc.ServiceDescriptor result = serviceDescriptor;
    if (result == null) {
      synchronized (FamilyServiceGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new FamilyServiceFileDescriptorSupplier())
              .addMethod(getJoinMethod())
              .addMethod(getGetFamilyMethod())
              .addMethod(getReceiveChatMethod())
              .build();
        }
      }
    }
    return result;
  }
}
