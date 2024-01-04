// Code generated by protoc-gen-go-grpc. DO NOT EDIT.
// versions:
// - protoc-gen-go-grpc v1.2.0
// - protoc             v4.25.1
// source: drives.proto

package drives

import (
	context "context"
	grpc "google.golang.org/grpc"
	codes "google.golang.org/grpc/codes"
	status "google.golang.org/grpc/status"
)

// This is a compile-time assertion to ensure that this generated file
// is compatible with the grpc package it is being compiled against.
// Requires gRPC-Go v1.32.0 or later.
const _ = grpc.SupportPackageIsVersion7

// DriveServiceClient is the client API for DriveService service.
//
// For semantics around ctx use and closing/ending streaming RPCs, please refer to https://pkg.go.dev/google.golang.org/grpc/?tab=doc#ClientConn.NewStream.
type DriveServiceClient interface {
	Create(ctx context.Context, in *CreateRequest, opts ...grpc.CallOption) (*CreateReply, error)
	Delete(ctx context.Context, in *DeleteRequest, opts ...grpc.CallOption) (*DeleteReply, error)
	Browse(ctx context.Context, in *BrowseRequest, opts ...grpc.CallOption) (*BrowseReply, error)
	UpdateAcl(ctx context.Context, in *UpdateAclRequest, opts ...grpc.CallOption) (*UpdateAclReply, error)
}

type driveServiceClient struct {
	cc grpc.ClientConnInterface
}

func NewDriveServiceClient(cc grpc.ClientConnInterface) DriveServiceClient {
	return &driveServiceClient{cc}
}

func (c *driveServiceClient) Create(ctx context.Context, in *CreateRequest, opts ...grpc.CallOption) (*CreateReply, error) {
	out := new(CreateReply)
	err := c.cc.Invoke(ctx, "/drives.DriveService/Create", in, out, opts...)
	if err != nil {
		return nil, err
	}
	return out, nil
}

func (c *driveServiceClient) Delete(ctx context.Context, in *DeleteRequest, opts ...grpc.CallOption) (*DeleteReply, error) {
	out := new(DeleteReply)
	err := c.cc.Invoke(ctx, "/drives.DriveService/Delete", in, out, opts...)
	if err != nil {
		return nil, err
	}
	return out, nil
}

func (c *driveServiceClient) Browse(ctx context.Context, in *BrowseRequest, opts ...grpc.CallOption) (*BrowseReply, error) {
	out := new(BrowseReply)
	err := c.cc.Invoke(ctx, "/drives.DriveService/Browse", in, out, opts...)
	if err != nil {
		return nil, err
	}
	return out, nil
}

func (c *driveServiceClient) UpdateAcl(ctx context.Context, in *UpdateAclRequest, opts ...grpc.CallOption) (*UpdateAclReply, error) {
	out := new(UpdateAclReply)
	err := c.cc.Invoke(ctx, "/drives.DriveService/UpdateAcl", in, out, opts...)
	if err != nil {
		return nil, err
	}
	return out, nil
}

// DriveServiceServer is the server API for DriveService service.
// All implementations must embed UnimplementedDriveServiceServer
// for forward compatibility
type DriveServiceServer interface {
	Create(context.Context, *CreateRequest) (*CreateReply, error)
	Delete(context.Context, *DeleteRequest) (*DeleteReply, error)
	Browse(context.Context, *BrowseRequest) (*BrowseReply, error)
	UpdateAcl(context.Context, *UpdateAclRequest) (*UpdateAclReply, error)
	mustEmbedUnimplementedDriveServiceServer()
}

// UnimplementedDriveServiceServer must be embedded to have forward compatible implementations.
type UnimplementedDriveServiceServer struct {
}

func (UnimplementedDriveServiceServer) Create(context.Context, *CreateRequest) (*CreateReply, error) {
	return nil, status.Errorf(codes.Unimplemented, "method Create not implemented")
}
func (UnimplementedDriveServiceServer) Delete(context.Context, *DeleteRequest) (*DeleteReply, error) {
	return nil, status.Errorf(codes.Unimplemented, "method Delete not implemented")
}
func (UnimplementedDriveServiceServer) Browse(context.Context, *BrowseRequest) (*BrowseReply, error) {
	return nil, status.Errorf(codes.Unimplemented, "method Browse not implemented")
}
func (UnimplementedDriveServiceServer) UpdateAcl(context.Context, *UpdateAclRequest) (*UpdateAclReply, error) {
	return nil, status.Errorf(codes.Unimplemented, "method UpdateAcl not implemented")
}
func (UnimplementedDriveServiceServer) mustEmbedUnimplementedDriveServiceServer() {}

// UnsafeDriveServiceServer may be embedded to opt out of forward compatibility for this service.
// Use of this interface is not recommended, as added methods to DriveServiceServer will
// result in compilation errors.
type UnsafeDriveServiceServer interface {
	mustEmbedUnimplementedDriveServiceServer()
}

func RegisterDriveServiceServer(s grpc.ServiceRegistrar, srv DriveServiceServer) {
	s.RegisterService(&DriveService_ServiceDesc, srv)
}

func _DriveService_Create_Handler(srv interface{}, ctx context.Context, dec func(interface{}) error, interceptor grpc.UnaryServerInterceptor) (interface{}, error) {
	in := new(CreateRequest)
	if err := dec(in); err != nil {
		return nil, err
	}
	if interceptor == nil {
		return srv.(DriveServiceServer).Create(ctx, in)
	}
	info := &grpc.UnaryServerInfo{
		Server:     srv,
		FullMethod: "/drives.DriveService/Create",
	}
	handler := func(ctx context.Context, req interface{}) (interface{}, error) {
		return srv.(DriveServiceServer).Create(ctx, req.(*CreateRequest))
	}
	return interceptor(ctx, in, info, handler)
}

func _DriveService_Delete_Handler(srv interface{}, ctx context.Context, dec func(interface{}) error, interceptor grpc.UnaryServerInterceptor) (interface{}, error) {
	in := new(DeleteRequest)
	if err := dec(in); err != nil {
		return nil, err
	}
	if interceptor == nil {
		return srv.(DriveServiceServer).Delete(ctx, in)
	}
	info := &grpc.UnaryServerInfo{
		Server:     srv,
		FullMethod: "/drives.DriveService/Delete",
	}
	handler := func(ctx context.Context, req interface{}) (interface{}, error) {
		return srv.(DriveServiceServer).Delete(ctx, req.(*DeleteRequest))
	}
	return interceptor(ctx, in, info, handler)
}

func _DriveService_Browse_Handler(srv interface{}, ctx context.Context, dec func(interface{}) error, interceptor grpc.UnaryServerInterceptor) (interface{}, error) {
	in := new(BrowseRequest)
	if err := dec(in); err != nil {
		return nil, err
	}
	if interceptor == nil {
		return srv.(DriveServiceServer).Browse(ctx, in)
	}
	info := &grpc.UnaryServerInfo{
		Server:     srv,
		FullMethod: "/drives.DriveService/Browse",
	}
	handler := func(ctx context.Context, req interface{}) (interface{}, error) {
		return srv.(DriveServiceServer).Browse(ctx, req.(*BrowseRequest))
	}
	return interceptor(ctx, in, info, handler)
}

func _DriveService_UpdateAcl_Handler(srv interface{}, ctx context.Context, dec func(interface{}) error, interceptor grpc.UnaryServerInterceptor) (interface{}, error) {
	in := new(UpdateAclRequest)
	if err := dec(in); err != nil {
		return nil, err
	}
	if interceptor == nil {
		return srv.(DriveServiceServer).UpdateAcl(ctx, in)
	}
	info := &grpc.UnaryServerInfo{
		Server:     srv,
		FullMethod: "/drives.DriveService/UpdateAcl",
	}
	handler := func(ctx context.Context, req interface{}) (interface{}, error) {
		return srv.(DriveServiceServer).UpdateAcl(ctx, req.(*UpdateAclRequest))
	}
	return interceptor(ctx, in, info, handler)
}

// DriveService_ServiceDesc is the grpc.ServiceDesc for DriveService service.
// It's only intended for direct use with grpc.RegisterService,
// and not to be introspected or modified (even as a copy)
var DriveService_ServiceDesc = grpc.ServiceDesc{
	ServiceName: "drives.DriveService",
	HandlerType: (*DriveServiceServer)(nil),
	Methods: []grpc.MethodDesc{
		{
			MethodName: "Create",
			Handler:    _DriveService_Create_Handler,
		},
		{
			MethodName: "Delete",
			Handler:    _DriveService_Delete_Handler,
		},
		{
			MethodName: "Browse",
			Handler:    _DriveService_Browse_Handler,
		},
		{
			MethodName: "UpdateAcl",
			Handler:    _DriveService_UpdateAcl_Handler,
		},
	},
	Streams:  []grpc.StreamDesc{},
	Metadata: "drives.proto",
}