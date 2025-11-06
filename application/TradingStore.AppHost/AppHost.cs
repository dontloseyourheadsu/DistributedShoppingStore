var builder = DistributedApplication.CreateBuilder(args);

var cache = builder.AddRedis("cache");

var apiService = builder.AddProject<Projects.TradingStore_ApiService>("apiservice")
    .WithHttpHealthCheck("/health");

// gRPC trading service
var grpcService = builder.AddProject<Projects.TradingStore_GrpcService>("grpcservice")
    .WithHttpHealthCheck("/health")
    .WithReference(cache)
    .WaitFor(cache);

builder.AddProject<Projects.TradingStore_Web>("webfrontend")
    .WithExternalHttpEndpoints()
    .WithHttpHealthCheck("/health")
    .WithReference(cache)
    .WaitFor(cache)
    .WithReference(apiService)
    .WaitFor(apiService)
    .WithReference(grpcService)
    .WaitFor(grpcService);

builder.Build().Run();
