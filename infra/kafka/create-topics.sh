#!/bin/bash
KAFKA_CONTAINER=smartcommerce-kafka
BROKER=kafka:29092

TOPICS=(
  "user.registered.v1"
  "user.profile-updated.v1"
  "user.deleted.v1"
  "seller.registered.v1"
  "seller.status-changed.v1"
  "product.created.v1"
  "product.updated.v1"
  "product.deleted.v1"
  "offer.created.v1"
  "offer.price-changed.v1"
  "offer.stock-changed.v1"
  "offer.status-changed.v1"
  "inventory.reserved.v1"
  "inventory.reservation-failed.v1"
  "inventory.released.v1"
  "inventory.confirmed.v1"
  "inventory.low-stock.v1"
  "order.created.v1"
  "order.confirmed.v1"
  "order.cancelled.v1"
  "order.expired.v1"
  "order.fraud-flagged.v1"
  "payment.initiated.v1"
  "payment.succeeded.v1"
  "payment.failed.v1"
  "payment.refunded.v1"
  "shipment.created.v1"
  "shipment.dispatched.v1"
  "shipment.delivered.v1"
  "shipment.failed.v1"
  "return.requested.v1"
  "return.approved.v1"
  "return.rejected.v1"
  "return.refunded.v1"
  "return.completed.v1"
  "cart.item-added.v1"
  "cart.checkout-started.v1"
  "cart.abandoned.v1"
  "review.created.v1"
  "review.updated.v1"
  "review.deleted.v1"
  "review.approved.v1"
  "review.rejected.v1"
)

for topic in "${TOPICS[@]}"; do
  docker exec $KAFKA_CONTAINER kafka-topics \
    --bootstrap-server $BROKER \
    --create \
    --if-not-exists \
    --topic "$topic" \
    --partitions 3 \
    --replication-factor 1
  docker exec $KAFKA_CONTAINER kafka-topics \
    --bootstrap-server $BROKER \
    --create \
    --if-not-exists \
    --topic "${topic}.dlq" \
    --partitions 1 \
    --replication-factor 1
done

echo "All topics created"
docker exec $KAFKA_CONTAINER kafka-topics --bootstrap-server $BROKER --list
