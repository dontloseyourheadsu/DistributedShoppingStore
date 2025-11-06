using Microsoft.Extensions.Hosting;
using Microsoft.Extensions.DependencyInjection;
using Microsoft.AspNetCore.Builder;
using Grpc.AspNetCore;
using TradingStore.GrpcService.Services;

var builder = WebApplication.CreateBuilder(args);

// Add service defaults & Aspire integrations (telemetry, health, discovery)
builder.AddServiceDefaults();

// Add services to the container.
builder.Services.AddGrpc();

var app = builder.Build();

// Configure the HTTP request pipeline.
app.MapGrpcService<GreeterService>();
app.MapGrpcService<TradingStore.GrpcService.Services.TradingServiceImpl>();
app.MapGet("/", () => "Communication with gRPC endpoints must be made through a gRPC client. To learn how to create a client, visit: https://go.microsoft.com/fwlink/?linkid=2086909");

app.MapDefaultEndpoints();

app.Run();
