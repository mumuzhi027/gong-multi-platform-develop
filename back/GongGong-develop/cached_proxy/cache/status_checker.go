package cache

import "time"

type ItemStatus int

const (
	Valid    = iota
	Expired
	NotFound
	Updating
)

type StatusChecker[V any] interface {
	StatusOf(item *cacheItem[V]) ItemStatus
}

type DailyStatusChecker[V any] struct {
	submitExpireInterval time.Duration
}

func sameCalendarDay(a, b time.Time) bool {
	if a.IsZero() || b.IsZero() {
		return false
	}
	ay, am, ad := a.Local().Date()
	by, bm, bd := b.Local().Date()
	return ay == by && am == bm && ad == bd
}

func (d *DailyStatusChecker[V]) StatusOf(item *cacheItem[V]) ItemStatus {
	if item == nil {
		return NotFound
	}

	now := time.Now()
	if sameCalendarDay(item.updateAt, now) {
		return Valid
	}
	if item.submitAt.Add(d.submitExpireInterval).After(now) {
		return Updating
	}
	return Expired
}

func NewDailyStatusChecker[V any](submitExpireAt time.Duration) *DailyStatusChecker[V] {
	return &DailyStatusChecker[V]{
		submitExpireInterval: submitExpireAt,
	}
}

type IntervalStatusChecker[V any] struct {
	updateExpireInterval time.Duration
	submitExpireInterval time.Duration
}

func (d *IntervalStatusChecker[V]) StatusOf(item *cacheItem[V]) ItemStatus {
	if item == nil {
		return NotFound
	}
	if item.updateAt.Add(d.updateExpireInterval).After(time.Now()) {
		return Valid
	}
	if item.submitAt.Add(d.submitExpireInterval).After(time.Now()) {
		return Updating
	}
	return Expired
}

func NewIntervalStatusChecker[V any](updateExpireAt, submitExpireAt time.Duration) *IntervalStatusChecker[V] {
	return &IntervalStatusChecker[V]{
		updateExpireInterval: updateExpireAt,
		submitExpireInterval: submitExpireAt,
	}
}
