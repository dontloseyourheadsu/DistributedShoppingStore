using System.Collections.Concurrent;
using Grpc.Core;

namespace TradingStore.GrpcService.Services;

public class TradingServiceImpl : TradingService.TradingServiceBase
{
    private static readonly ConcurrentDictionary<string, RoomState> Rooms = new();
    private static readonly ConcurrentDictionary<string, ItemState> Items = new();
    private static readonly ConcurrentDictionary<string, List<Bid>> BidHistory = new();

    public override Task<CreateRoomReply> CreateRoom(CreateRoomRequest request, ServerCallContext context)
    {
        var roomId = Guid.NewGuid().ToString("N");
        var room = new RoomState
        {
            Id = roomId,
            Name = request.Name,
            Category = request.Category,
            SellerId = request.SellerId,
            IsPrivate = request.IsPrivate,
            Password = request.Password ?? string.Empty,
            Capacity = request.Capacity > 0 ? request.Capacity : 5
        };
        Rooms[roomId] = room;
        return Task.FromResult(new CreateRoomReply { RoomId = roomId });
    }

    public override Task<JoinRoomReply> JoinRoom(JoinRoomRequest request, ServerCallContext context)
    {
        if (!Rooms.TryGetValue(request.RoomId, out var room))
            return Task.FromResult(new JoinRoomReply { Success = false, Error = "room_not_found" });

        if (room.IsPrivate && room.Password != (request.Password ?? string.Empty))
            return Task.FromResult(new JoinRoomReply { Success = false, Error = "forbidden" });

        if (room.ActiveMembers >= room.Capacity)
            return Task.FromResult(new JoinRoomReply { Success = false, Error = "room_full" });

        room.ActiveMembers++;
        return Task.FromResult(new JoinRoomReply { Success = true });
    }

    public override Task<CreateItemReply> CreateItem(CreateItemRequest request, ServerCallContext context)
    {
        if (!Rooms.ContainsKey(request.RoomId))
            throw new RpcException(new Status(StatusCode.NotFound, "room_not_found"));

        var itemId = Guid.NewGuid().ToString("N");
        var item = new ItemState
        {
            Id = itemId,
            RoomId = request.RoomId,
            Name = request.Name,
            Description = request.Description ?? string.Empty,
            StartPrice = request.StartPrice,
            CurrentPrice = request.StartPrice,
            Version = 1
        };
        Items[itemId] = item;
        BidHistory[itemId] = new List<Bid>();
        return Task.FromResult(new CreateItemReply { ItemId = itemId });
    }

    public override Task<GetRoomItemsReply> GetRoomItems(GetRoomItemsRequest request, ServerCallContext context)
    {
        var items = Items.Values.Where(i => i.RoomId == request.RoomId)
            .Select(i => new Item
            {
                Id = i.Id,
                RoomId = i.RoomId,
                Name = i.Name,
                Description = i.Description,
                StartPrice = i.StartPrice,
                CurrentPrice = i.CurrentPrice,
                Version = i.Version
            });
        var reply = new GetRoomItemsReply();
        reply.Items.AddRange(items);
        return Task.FromResult(reply);
    }

    public override Task<PlaceBidReply> PlaceBid(PlaceBidRequest request, ServerCallContext context)
    {
        if (!Items.TryGetValue(request.ItemId, out var item))
            return Task.FromResult(new PlaceBidReply { Accepted = false, Error = "item_not_found" });

        // optimistic concurrency
        if (request.Version != item.Version)
        {
            return Task.FromResult(new PlaceBidReply
            {
                Accepted = false,
                Error = "version_conflict",
                CurrentPrice = item.CurrentPrice,
                CurrentVersion = item.Version
            });
        }

        if (request.Amount <= item.CurrentPrice)
        {
            return Task.FromResult(new PlaceBidReply
            {
                Accepted = false,
                Error = "amount_too_low",
                CurrentPrice = item.CurrentPrice,
                CurrentVersion = item.Version
            });
        }

        item.CurrentPrice = request.Amount;
        item.Version++;

        var bid = new Bid
        {
            Id = Guid.NewGuid().ToString("N"),
            ItemId = item.Id,
            BuyerId = request.BuyerId,
            Amount = request.Amount,
            Timestamp = DateTimeOffset.UtcNow.ToUnixTimeSeconds(),
            Version = item.Version
        };
        BidHistory[item.Id].Add(bid);

        return Task.FromResult(new PlaceBidReply
        {
            Accepted = true,
            CurrentPrice = item.CurrentPrice,
            CurrentVersion = item.Version
        });
    }

    public override Task<GetBidHistoryReply> GetBidHistory(GetBidHistoryRequest request, ServerCallContext context)
    {
        var list = BidHistory.TryGetValue(request.ItemId, out var hist) ? hist : new List<Bid>();
        var reply = new GetBidHistoryReply();
        reply.Bids.AddRange(list);
        return Task.FromResult(reply);
    }

    private class RoomState
    {
        public string Id { get; set; } = string.Empty;
        public string Name { get; set; } = string.Empty;
        public string Category { get; set; } = string.Empty;
        public string SellerId { get; set; } = string.Empty;
        public bool IsPrivate { get; set; }
        public string Password { get; set; } = string.Empty;
        public int Capacity { get; set; } = 5;
        public int ActiveMembers { get; set; }
    }

    private class ItemState
    {
        public string Id { get; set; } = string.Empty;
        public string RoomId { get; set; } = string.Empty;
        public string Name { get; set; } = string.Empty;
        public string Description { get; set; } = string.Empty;
        public double StartPrice { get; set; }
        public double CurrentPrice { get; set; }
        public int Version { get; set; }
    }
}
